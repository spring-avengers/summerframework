package com.bkjk.platform.jobcenter;

import com.bkjk.platform.jobcenter.discovery.util.InetUtils;
import com.xxl.job.core.executor.XxlJobExecutor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@Configuration
@EnableConfigurationProperties({JobCenterProperties.class})
public class JobCenterAutoConfiguration {

    static class ApplicationHome {

        private final File source;

        private final File dir;

        /**
         * Create a new {@link ApplicationHome} instance.
         */
        public ApplicationHome() {
            this(null);
        }

        /**
         * Create a new {@link ApplicationHome} instance for the specified source class.
         * 
         * @param sourceClass the source class or {@code null}
         */
        public ApplicationHome(Class<?> sourceClass) {
            this.source = findSource(sourceClass != null ? sourceClass : getStartClass());
            this.dir = findHomeDir(this.source);
        }

        private File findDefaultHomeDir() {
            String userDir = System.getProperty("user.dir");
            return new File(org.springframework.util.StringUtils.hasLength(userDir) ? userDir : ".");
        }

        private File findHomeDir(File source) {
            File homeDir = source;
            homeDir = (homeDir != null ? homeDir : findDefaultHomeDir());
            if (homeDir.isFile()) {
                homeDir = homeDir.getParentFile();
            }
            homeDir = (homeDir.exists() ? homeDir : new File("."));
            return homeDir.getAbsoluteFile();
        }

        private File findSource(Class<?> sourceClass) {
            try {
                ProtectionDomain domain = (sourceClass != null ? sourceClass.getProtectionDomain() : null);
                CodeSource codeSource = (domain != null ? domain.getCodeSource() : null);
                URL location = (codeSource != null ? codeSource.getLocation() : null);
                File source = (location != null ? findSource(location) : null);
                if (source != null && source.exists() && !isUnitTest()) {
                    return source.getAbsoluteFile();
                }
                return null;
            } catch (Exception ex) {
                return null;
            }
        }

        private File findSource(URL location) throws IOException {
            URLConnection connection = location.openConnection();
            if (connection instanceof JarURLConnection) {
                return getRootJarFile(((JarURLConnection)connection).getJarFile());
            }
            return new File(location.getPath());
        }

        /**
         * Returns the application home directory.
         * 
         * @return the home directory (never {@code null})
         */
        public File getDir() {
            return this.dir;
        }

        private File getRootJarFile(JarFile jarFile) {
            String name = jarFile.getName();
            int separator = name.indexOf("!/");
            if (separator > 0) {
                name = name.substring(0, separator);
            }
            return new File(name);
        }

        /**
         * Returns the underlying source used to find the home directory. This is usually the jar file or a directory.
         * Can return {@code null} if the source cannot be determined.
         * 
         * @return the underlying source or {@code null}
         */
        public File getSource() {
            return this.source;
        }

        private Class<?> getStartClass() {
            try {
                ClassLoader classLoader = getClass().getClassLoader();
                return getStartClass(classLoader.getResources("META-INF/MANIFEST.MF"));
            } catch (Exception ex) {
                return null;
            }
        }

        private Class<?> getStartClass(Enumeration<URL> manifestResources) {
            while (manifestResources.hasMoreElements()) {
                try {
                    InputStream inputStream = manifestResources.nextElement().openStream();
                    try {
                        Manifest manifest = new Manifest(inputStream);
                        String startClass = manifest.getMainAttributes().getValue("Start-Class");
                        if (startClass != null) {
                            return ClassUtils.forName(startClass, getClass().getClassLoader());
                        }
                    } finally {
                        inputStream.close();
                    }
                } catch (Exception ex) {
                }
            }
            return null;
        }

        private boolean isUnitTest() {
            try {
                for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                    if (element.getClassName().startsWith("org.junit.")) {
                        return true;
                    }
                }
            } catch (Exception ex) {
            }
            return false;
        }

        @Override
        public String toString() {
            return getDir().toString();
        }

    }

    private static class NamedThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

        private final AtomicInteger mThreadNum = new AtomicInteger(1);

        private final String mPrefix;

        private final boolean mDaemon;

        private final ThreadGroup mGroup;

        public NamedThreadFactory() {
            this("XxlJobSchedule-" + POOL_SEQ.getAndIncrement(), true);
        }

        public NamedThreadFactory(String prefix, boolean daemon) {
            mPrefix = prefix + "-thread-";
            mDaemon = daemon;
            SecurityManager s = System.getSecurityManager();
            mGroup = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable runnable) {
            String name = mPrefix + mThreadNum.getAndIncrement();
            Thread ret = new Thread(mGroup, runnable, name, 0);
            ret.setDaemon(mDaemon);
            return ret;
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JobCenterAutoConfiguration.class);

    static final CloseableHttpClient httpClient = HttpClients.custom().disableAutomaticRetries().build();

    @Autowired
    private JobCenterProperties jobCenterProperties;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    private String appName;

    private String discoveryAdminAddress() {
        String serviceId = jobCenterProperties.getServiceId();
        List<String> adminAddresList = new ArrayList<>();
        List<ServiceInstance> instanceList = discoveryClient.getInstances(serviceId);
        for (ServiceInstance serviceInstance : instanceList) {
            try {
                String host = serviceInstance.getHost();
                int port = serviceInstance.getPort();
                LOGGER.info("Connecting {} ({}:{})", serviceId, host, port);
                String httpAddress = "http://" + host + ":" + port + "/";
                if (isAdminReachable(httpAddress)) {
                    adminAddresList.add(httpAddress);
                } else {
                    LOGGER.error("Skip unreachable node {}", httpAddress);
                }
            } catch (Throwable e) {
                LOGGER.error("can not found node for admin!", e);
            }
        }
        if (adminAddresList.size() > 0) {
            adminAddresList.sort(Comparator.naturalOrder());
            return String.join(",", adminAddresList);
        } else {
            LOGGER.error("Jobcenter server is down,will try after 30s");
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private String discoveryLocalIp() {
        if (!StringUtils.isEmpty(jobCenterProperties.getLocalIp())) {
            LOGGER.error("should not run in PROD");
            InetAddress result = null;
            try {
                int lowest = Integer.MAX_VALUE;
                for (Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
                    nics.hasMoreElements();) {
                    NetworkInterface ifc = nics.nextElement();
                    if (ifc.isUp()) {
                        LOGGER.error("Testing interface: " + ifc.getDisplayName());
                        if (ifc.getIndex() < lowest || result == null) {
                            lowest = ifc.getIndex();
                        } else if (result != null) {
                            continue;
                        }
                        for (Enumeration<InetAddress> addrs = ifc.getInetAddresses(); addrs.hasMoreElements();) {
                            InetAddress address = addrs.nextElement();
                            if (address instanceof Inet4Address && !address.isLoopbackAddress()
                                && address.getHostAddress().equals(jobCenterProperties.getLocalIp())) {
                                LOGGER.error("Use localIp " + address.getHostAddress());
                                return address.getHostAddress();
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                LOGGER.error("Cannot get first non-loopback address", ex);
            }
        }
        return InetUtils.getFirstNonLoopbackHostInfo().getIpAddress();
    }

    private String getCallBackLogPath() {
        ApplicationHome home = new ApplicationHome(JobCenterAutoConfiguration.class);
        File path = home.getDir();
        String logFilePath = path.getPath() + "/logs/xxljob";
        File logFile = new File(logFilePath);
        this.mkdir(logFile);
        return logFilePath;
    }

    @PostConstruct
    public void init() {
        this.initDefaultParm();
        ScheduledExecutorService scheduleReport = Executors.newScheduledThreadPool(1, new NamedThreadFactory());
        scheduleReport.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    XxlJobExecutor xxlJobExecutor = applicationContext.getBean(XxlJobExecutor.class);
                    String adminAddresses = discoveryAdminAddress();
                    if (StringUtils.isEmpty(adminAddresses)) {
                        return;
                    }
                    if (adminAddresses.equals(xxlJobExecutor.getAdminAddresses())) {
                        LOGGER.info("AdminAddresses has no changes.");
                        return;
                    }
                    if (xxlJobExecutor.getAdminAddresses() == null) {
                        LOGGER.info("Old adminAddresses is NULL");
                    } else {
                        LOGGER.info("Remove old node {}", xxlJobExecutor.getAdminAddresses());
                        XxlJobExecutor.getAdminBizList().clear();
                    }

                    LOGGER.info("Find all adminAddresses:" + adminAddresses + " from eureka");
                    xxlJobExecutor.setAdminAddresses(adminAddresses);
                    xxlJobExecutor.reInitAdminBizList();
                } catch (Throwable e) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    public void initDefaultParm() {
        // appName
        String propertiesAppName = jobCenterProperties.getAppName();
        if (StringUtils.isBlank(propertiesAppName)) {
            propertiesAppName = environment.getProperty("spring.application.name");
        }
        if (StringUtils.isBlank(propertiesAppName)) {
            throw new RuntimeException("appName is null, You must config xxljob excutor name !!");
        } else {
            this.appName = propertiesAppName;
        }
    }

    private boolean isAdminReachable(String httpAddress) {
        HttpGet httpGet = new HttpGet(httpAddress);
        try {
            CloseableHttpResponse response = httpClient.execute(httpGet);
            EntityUtils.consume(response.getEntity());
            return response.getStatusLine().getStatusCode() == 200;
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        } finally {
            httpGet.releaseConnection();
        }
    }

    private void mkdir(File file) {
        if (file.getParentFile().exists()) {
            file.mkdir();
        } else {
            mkdir(file.getParentFile());
            file.mkdir();
        }
    }

    @Bean(initMethod = "start", destroyMethod = "destroy")
    public XxlJobExecutor xxlJobExecutor() {
        LOGGER.info(">>>>>>>>>>> Jobcenter config init.");
        String adminAddresses = this.discoveryAdminAddress();
        String localIp = this.discoveryLocalIp();
        int port = jobCenterProperties.getPort();
        String accessToken = jobCenterProperties.getAccessToken();
        String callBackPath =
            jobCenterProperties.getLogPath() != null ? jobCenterProperties.getLogPath() : this.getCallBackLogPath();
        XxlJobExecutor xxlJobExecutor = new XxlJobExecutor();
        xxlJobExecutor.setAdminAddresses(adminAddresses);
        xxlJobExecutor.setAppName(appName);
        xxlJobExecutor.setIp(localIp);
        xxlJobExecutor.setPort(port);
        xxlJobExecutor.setAccessToken(accessToken);
        xxlJobExecutor.setLogPath(callBackPath);
        xxlJobExecutor.setLogRetentionDays(-1);
        xxlJobExecutor.setApplicationContext(applicationContext);
        return xxlJobExecutor;
    }

}
