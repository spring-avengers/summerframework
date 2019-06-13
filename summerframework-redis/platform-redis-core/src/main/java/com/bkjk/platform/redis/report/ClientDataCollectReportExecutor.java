package com.bkjk.platform.redis.report;

import static com.bkjk.platform.redis.report.CacheCloudInfoHttpHelper.CACHECLOUD_REPORT_URL;
import static com.bkjk.platform.redis.report.CacheCloudInfoHttpHelper.CLIENT_VERSION;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sohu.tv.cachecloud.client.basic.util.DateUtils;
import com.sohu.tv.cachecloud.client.basic.util.HttpUtils;
import com.sohu.tv.cachecloud.client.basic.util.JsonUtil;
import com.sohu.tv.cachecloud.client.basic.util.NamedThreadFactory;
import com.sohu.tv.cachecloud.client.basic.util.NetUtils;
import com.sohu.tv.jedis.stat.constant.ClientReportConstant;
import com.sohu.tv.jedis.stat.data.UsefulDataCollector;
import com.sohu.tv.jedis.stat.enums.ClientCollectDataTypeEnum;
import com.sohu.tv.jedis.stat.enums.ClientExceptionType;
import com.sohu.tv.jedis.stat.model.ClientReportBean;
import com.sohu.tv.jedis.stat.model.CostTimeDetailStatKey;
import com.sohu.tv.jedis.stat.model.CostTimeDetailStatModel;
import com.sohu.tv.jedis.stat.model.ExceptionModel;
import com.sohu.tv.jedis.stat.model.ValueLengthModel;
import com.sohu.tv.jedis.stat.utils.AtomicLongMap;
import com.sohu.tv.jedis.stat.utils.NumberUtil;

public class ClientDataCollectReportExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDataCollectReportExecutor.class);

    private static String CLIENT_IP = NetUtils.getLocalHost();

    private volatile static ClientDataCollectReportExecutor jedisDataCollectAndReportExecutor;

    public static ClientDataCollectReportExecutor getInstance() {
        if (jedisDataCollectAndReportExecutor == null) {
            synchronized (ClientDataCollectReportExecutor.class) {
                if (jedisDataCollectAndReportExecutor == null) {
                    jedisDataCollectAndReportExecutor = new ClientDataCollectReportExecutor();
                }
            }
        }
        return jedisDataCollectAndReportExecutor;
    }

    public static void reportData(ClientReportBean cReportBean) {
        if (cReportBean == null) {
            LOGGER.error("cReportBean is null!");
        }

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(ClientReportConstant.JSON_PARAM, JsonUtil.toJson(cReportBean));
        parameters.put(ClientReportConstant.CLIENT_VERSION, CLIENT_VERSION);

        try {
            HttpUtils.doPost(CACHECLOUD_REPORT_URL, parameters);
        } catch (Exception e) {
            LOGGER.error("cachecloud reportData exception: " + e.getMessage());
            UsefulDataCollector.collectException(e, "", System.currentTimeMillis(),
                ClientExceptionType.CLIENT_EXCEPTION_TYPE);
        }
    }

    private final ScheduledExecutorService jedisDataCollectReportScheduledExecutor =
        Executors.newScheduledThreadPool(3, new NamedThreadFactory("jedisDataCollectReportScheduledExecutor", true));

    private ScheduledFuture<?> jedisDataCollectReportScheduleFuture;

    private final int delay = 5;

    private final int fixCycle = 60;

    private ClientDataCollectReportExecutor() {
        init();
    }

    public void close() {
        try {
            jedisDataCollectReportScheduleFuture.cancel(true);
        } catch (Throwable t) {
            LOGGER.error(t.getMessage(), t);
        }
    }

    private void collectReportAllData(String currentMinuteStamp) {

        String lastMinute = getLastMinute(currentMinuteStamp);
        if (lastMinute == null || "".equals(lastMinute.trim())) {
            return;
        }
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        list.addAll(collectReportCostTimeData(lastMinute));
        list.addAll(collectReportValueDistriData(lastMinute));
        list.addAll(collectReportExceptionData(lastMinute));

        Map<String, Object> otherInfo = new HashMap<String, Object>(4, 1);
        otherInfo.put(ClientReportConstant.COST_MAP_SIZE, UsefulDataCollector.getDataCostTimeMapAll().size());
        otherInfo.put(ClientReportConstant.VALUE_MAP_SIZE,
            UsefulDataCollector.getDataValueLengthDistributeMapAll().size());
        otherInfo.put(ClientReportConstant.EXCEPTION_MAP_SIZE, UsefulDataCollector.getDataExceptionMapAll().size());
        otherInfo.put(ClientReportConstant.COLLECTION_MAP_SIZE,
            UsefulDataCollector.getCollectionCostTimeMapAll().size());

        if (!list.isEmpty()) {
            ClientReportBean ccReportBean = new ClientReportBean(CLIENT_IP, NumberUtil.toLong(lastMinute),
                System.currentTimeMillis(), list, otherInfo);
            reportData(ccReportBean);
        }
    }

    private List<Map<String, Object>> collectReportCostTimeData(String lastMinute) {
        try {

            Map<CostTimeDetailStatKey, AtomicLongMap<Integer>> map =
                UsefulDataCollector.getCostTimeLastMinute(lastMinute);
            if (map == null || map.isEmpty()) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (Map.Entry<CostTimeDetailStatKey, AtomicLongMap<Integer>> entry : map.entrySet()) {
                CostTimeDetailStatKey costTimeDetailStatKey = entry.getKey();
                AtomicLongMap<Integer> statMap = entry.getValue();
                CostTimeDetailStatModel model = UsefulDataCollector.generateCostTimeDetailStatKey(statMap);

                Map<String, Object> tempMap = new HashMap<String, Object>();
                tempMap.put(ClientReportConstant.COST_COUNT, model.getTotalCount());
                tempMap.put(ClientReportConstant.COST_COMMAND, costTimeDetailStatKey.getCommand());
                tempMap.put(ClientReportConstant.COST_HOST_PORT, costTimeDetailStatKey.getHostPort());
                tempMap.put(ClientReportConstant.COST_TIME_90_MAX, model.getNinetyPercentMax());
                tempMap.put(ClientReportConstant.COST_TIME_99_MAX, model.getNinetyNinePercentMax());
                tempMap.put(ClientReportConstant.COST_TIME_100_MAX, model.getHundredMax());
                tempMap.put(ClientReportConstant.COST_TIME_MEAN, model.getMean());
                tempMap.put(ClientReportConstant.COST_TIME_MEDIAN, model.getMedian());
                tempMap.put(ClientReportConstant.CLIENT_DATA_TYPE,
                    ClientCollectDataTypeEnum.COST_TIME_DISTRI_TYPE.getValue());
                list.add(tempMap);
            }
            return list;
        } catch (Exception e) {
            UsefulDataCollector.collectException(e, "", System.currentTimeMillis(),
                ClientExceptionType.CLIENT_EXCEPTION_TYPE);
            LOGGER.error("collectReportCostTimeData:" + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> collectReportExceptionData(String lastMinute) {
        try {

            Map<ExceptionModel, Long> map = UsefulDataCollector.getExceptionLastMinute(lastMinute);
            if (map == null || map.isEmpty()) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            Map<String, Object> tempMap = null;
            for (Map.Entry<ExceptionModel, Long> entry : map.entrySet()) {
                ExceptionModel exceptionModel = entry.getKey();
                Long exceptionCount = entry.getValue();
                tempMap = new HashMap<String, Object>();
                tempMap.put(ClientReportConstant.EXCEPTION_CLASS, exceptionModel.getExceptionClass());
                tempMap.put(ClientReportConstant.EXCEPTION_MSG, "");
                tempMap.put(ClientReportConstant.EXCEPTION_HAPPEN_TIME, System.currentTimeMillis());
                tempMap.put(ClientReportConstant.EXCEPTION_HOST_PORT, exceptionModel.getHostPort());
                tempMap.put(ClientReportConstant.EXCEPTION_COUNT, exceptionCount);
                tempMap.put(ClientReportConstant.EXCEPTION_TYPE, exceptionModel.getClientExceptionType().getType());
                tempMap.put(ClientReportConstant.CLIENT_DATA_TYPE, ClientCollectDataTypeEnum.EXCEPTION_TYPE.getValue());
                list.add(tempMap);
            }
            return list;
        } catch (Exception e) {
            UsefulDataCollector.collectException(e, "", System.currentTimeMillis(),
                ClientExceptionType.CLIENT_EXCEPTION_TYPE);
            LOGGER.error("collectReportExceptionData:" + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> collectReportValueDistriData(String lastMinute) {
        try {

            Map<ValueLengthModel, Long> jedisValueLengthMap = UsefulDataCollector.getValueLengthLastMinute(lastMinute);
            if (jedisValueLengthMap == null || jedisValueLengthMap.isEmpty()) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (Map.Entry<ValueLengthModel, Long> entry : jedisValueLengthMap.entrySet()) {
                ValueLengthModel model = entry.getKey();
                Long count = entry.getValue();
                Map<String, Object> tempMap = new HashMap<String, Object>();
                tempMap.put(ClientReportConstant.VALUE_DISTRI, model.getRedisValueSizeEnum().getValue());
                tempMap.put(ClientReportConstant.VALUE_COUNT, count);
                tempMap.put(ClientReportConstant.VALUE_COMMAND, model.getCommand());
                tempMap.put(ClientReportConstant.VALUE_HOST_PORT, model.getHostPort());
                tempMap.put(ClientReportConstant.CLIENT_DATA_TYPE,
                    ClientCollectDataTypeEnum.VALUE_LENGTH_DISTRI_TYPE.getValue());
                list.add(tempMap);
            }
            return list;
        } catch (Exception e) {
            UsefulDataCollector.collectException(e, "", System.currentTimeMillis(),
                ClientExceptionType.CLIENT_EXCEPTION_TYPE);
            LOGGER.error("collectReportValueDistriData:" + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String getLastMinute(String currentMinuteStamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            Date currentDate = sdf.parse(currentMinuteStamp);
            Date lastMinute = DateUtils.addMinutes(currentDate, -1);
            return sdf.format(lastMinute);
        } catch (ParseException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    public void init() {
        Thread clientDataCollectReportThread = new Thread(() -> {
            try {
                String currentMinuteStamp = ClientReportConstant.getCollectTimeSDf().format(new Date());
                collectReportAllData(currentMinuteStamp);
            } catch (Exception e) {
                UsefulDataCollector.collectException(e, "", System.currentTimeMillis(),
                    ClientExceptionType.CLIENT_EXCEPTION_TYPE);
                LOGGER.error("ClientDataCollectReport thread message is" + e.getMessage(), e);
            }
        });
        clientDataCollectReportThread.setDaemon(true);

        jedisDataCollectReportScheduleFuture = jedisDataCollectReportScheduledExecutor
            .scheduleWithFixedDelay(clientDataCollectReportThread, delay, fixCycle, TimeUnit.SECONDS);
    }
}
