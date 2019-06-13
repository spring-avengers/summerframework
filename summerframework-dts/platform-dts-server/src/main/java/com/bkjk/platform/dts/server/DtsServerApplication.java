
package com.bkjk.platform.dts.server;

import com.bkjk.platform.dts.server.health.MetricsHttpServer;
import com.bkjk.platform.dts.server.remoting.DtsRemotingServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.net.*;
import java.util.Collections;

import static com.hazelcast.util.EmptyStatement.ignore;

@EnableEurekaClient
@SpringBootApplication
@EnableScheduling
public class DtsServerApplication implements CommandLineRunner {

    private static final int MAX_PORT = 100;

    @Value("${server.port}")
    private int port = 0;

    static {
        System.setProperty("hazelcast.port", String.valueOf(getNextPort(5701)));
        System.setProperty("hazelcast.host", findHostRemoteIp());
    }

    private static String findHostRemoteIp() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
                    if (inetAddress instanceof Inet4Address) {
                        if (!"127.0.0.1".equals(inetAddress.getHostAddress())) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    private static int getNextPort(int start) {
        for (int port = start; port < start + MAX_PORT; port++) {
            try {
                new ServerSocket(port).close();
                return port;
            } catch (IOException portInUse) {
                ignore(portInUse);
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = SpringApplication.run(DtsServerApplication.class, args);
        DtsRemotingServer remotingServer = context.getBean(DtsRemotingServer.class);
        remotingServer.start();
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            new MetricsHttpServer(port + 1, true);
        } catch (IOException e) {
            ignore(e);
        }
    }

}
