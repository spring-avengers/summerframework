package com.bkjk.platform.rabbit.logger;

import com.bkjk.platform.rabbit.async.AsynchronousFlusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DatabaseMySQLTraceLogFlushHandler implements AsynchronousFlusher.Handler<MessageTraceBean> {

    public static final Logger logger = LoggerFactory.getLogger(DatabaseMySQLTraceLogFlushHandler.class);
    private static final int MAX_INDEX_KEY_LEN_IN_MYSQL = 191;

    private static final String CHECK_EXPIRED = "select 1 from message_trace where create_timestamp < ? LIMIT 1;";
    private static final String DELETE_BY_DAY = "delete from message_trace where create_timestamp BETWEEN ? AND ?";
    private static final String INSERT =
        "INSERT INTO `message_trace` (`id`, `application_name`, `channel`, `client_ip`, `connection`, `exchange`, `message_id`, `node`, `payload`, `properties`, `queue`, `routing_keys`, `success`, `timestamp`, `type`, `user`, `vhost`)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public static void main(String[] args) {
        System.out.println(CREATE_TABLE_IF_NOT_EXIST);
    }
    private static final String CREATE_TABLE_IF_NOT_EXIST =
        "CREATE TABLE IF NOT EXISTS `message_trace` ( `id` BIGINT (20) NOT NULL AUTO_INCREMENT, `application_name` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `channel` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `client_ip` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `connection` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `exchange` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `message_id` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `node` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `payload` VARCHAR (2000) DEFAULT NULL, `properties` VARCHAR (2000) DEFAULT NULL, `queue` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `routing_keys` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `success` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `timestamp` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `type` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `user` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `vhost` VARCHAR (" +MAX_INDEX_KEY_LEN_IN_MYSQL+ ") DEFAULT NULL, `create_timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY (`id`), KEY `application_name` ( `application_name`, `exchange`, `routing_keys` ), KEY `application_name_2` ( `application_name`, `routing_keys`, `queue` ), KEY `idx_create_timestamp` (`create_timestamp`)) ENGINE = INNODB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4;";

    private static final String SELECT_ALL_COLUMN_FROM_TABLE="select `id`, `application_name`, `channel`, `client_ip`, `connection`, `exchange`, `message_id`, `node`, `payload`, `properties`, `queue`, `routing_keys`, `success`, `timestamp`, `type`, `user`, `vhost` from message_trace limit 1";

    private static final String COUNT = "select count(0) from message_trace";
    private static final String SELECT_PAGE = "SELECT * from message_trace limit ?,?";

    public static final Consumer<Connection> createTableIfNotExist = (connection -> {
        // 先检测表是否存在
        try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL_COLUMN_FROM_TABLE)) {
            try(ResultSet rs=ps.executeQuery()){
                rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking table [message_trace]",e);
            // 如果报错则尝试创建表
            try (PreparedStatement ps = connection.prepareStatement(CREATE_TABLE_IF_NOT_EXIST)) {
                ps.execute();
            } catch (SQLException ex) {
                logger.error("Error creating table [message_trace].",ex);
                logger.warn("Using Slf4jTraceLogger");
                AbstractTraceLog.setTraceLogger(Slf4jTraceLogger.instance);
            }
        }
    });

    private static final String cutString(String string, int max) {
        if (StringUtils.isEmpty(string)) {
            return "";
        }
        return string.length() > max ? string.substring(0, max - 1) : string;
    }

    final DataSource dataSource;
    boolean tableReady;
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    int expiredDay = 7;

    private Thread cleanerTask = new Thread(() -> {
        doWithConnection(connection -> {
            try {
                int maxCount = 30;
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDateTime dayToClean = LocalDateTime.now().minusDays(expiredDay);
                int count = 0;

                boolean hasExpired = true;
                while (hasExpired) {
                    hasExpired = false;
                    try (PreparedStatement ps = connection.prepareStatement(CHECK_EXPIRED)) {
                        ps.setString(1, dayToClean.format(dateTimeFormatter));
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                hasExpired = rs.getInt(1) == 1;
                            }
                        }
                    }
                    try (PreparedStatement ps = connection.prepareStatement(DELETE_BY_DAY)) {
                        ps.setString(1, dayToClean.minusDays(count + 1).format(dateTimeFormatter));
                        ps.setString(2, dayToClean.minusDays(count).format(dateTimeFormatter));
                        ps.execute();
                    }
                    count++;
                    if (count > maxCount) {
                        logger.error("Too many expired data(more than 30 days!),try to clean them manually.");
                        break;
                    }
                }
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            }
        });
    });

    public DatabaseMySQLTraceLogFlushHandler(DataSource dataSource) {
        Assert.notNull(dataSource, "DatabaseTraceLogFlushHandler must have dataSource.");
        this.dataSource = dataSource;
        doWithConnection(createTableIfNotExist);
        tableReady = true;
        threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setDaemon(true);
        threadPoolTaskScheduler.setThreadNamePrefix("TraceLogFlush");
        threadPoolTaskScheduler.initialize();
        threadPoolTaskScheduler.schedule(cleanerTask,
            new CronTrigger(System.getProperty("rabbit.trace.flush.trigger", "0 0 1 * * ?")));
    }

    @Override
    public void batch(Collection<MessageTraceBean> list) {
        if (!tableReady) {
            logger.error("Creating table failed.");
            list.forEach(s -> {
                logger.info(s.toString());
            });
            return;
        }
        doWithConnection(connection -> {
            try {

                try (PreparedStatement ps = connection.prepareStatement(INSERT)) {
                    list.forEach(trace -> {
                        try {
                            int i = 1;
                            ps.setObject(i++, null);
                            ps.setString(i++, trace.getApplicationName());
                            ps.setString(i++, trace.getChannel());
                            ps.setString(i++, trace.getClientIp());
                            ps.setString(i++, trace.getConnection());
                            ps.setString(i++, trace.getExchange());
                            ps.setString(i++, trace.getMessageId());
                            ps.setString(i++, trace.getNode());
                            ps.setString(i++, cutString(trace.getPayload(), 2000));
                            ps.setString(i++, cutString(trace.getProperties(), 2000));
                            ps.setString(i++, trace.getQueue());
                            ps.setString(i++, trace.getRoutingKeys());
                            ps.setString(i++, trace.getSuccess());
                            ps.setString(i++, trace.getTimestamp());
                            ps.setString(i++, trace.getType());
                            ps.setString(i++, trace.getUser());
                            ps.setString(i++, trace.getVhost());

                            ps.addBatch();
                        } catch (SQLException e) {
                            logger.error(e.getMessage(), e);
                        }
                    });
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    private void doWithConnection(Consumer<Connection> consumer) {
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                consumer.accept(connection);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void onQueueFull(MessageTraceBean messageTraceBean) {
        logger.warn("Queue is full. Discard trace. {}", messageTraceBean.toString());
    }

    /***
     * @Description: 分页查询
     * @Param: [page, size]
     * @Return: java.util.List<com.bkjk.platform.rabbit.logger.MessageTraceBean>
     * @Author: shaoze.wang
     * @Date: 2019/2/19 13:43
     */
    public List<MessageTraceBean> select(int page, int size) {
        int offset = (page - 1) * size;
        List<MessageTraceBean> messageTraceBeans = new ArrayList<>();
        doWithConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_PAGE)) {
                ps.setInt(1, offset);
                ps.setInt(2, size);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        MessageTraceBean messageTraceBean = new MessageTraceBean();
                        messageTraceBean.setApplicationName(rs.getString("application_name"));
                        messageTraceBean.setChannel(rs.getString("channel"));
                        messageTraceBean.setClientIp(rs.getString("client_ip"));
                        messageTraceBean.setConnection(rs.getString("connection"));
                        messageTraceBean.setExchange(rs.getString("exchange"));
                        messageTraceBean.setMessageId(rs.getString("message_id"));
                        messageTraceBean.setNode(rs.getString("node"));
                        messageTraceBean.setPayload(rs.getString("payload"));
                        messageTraceBean.setProperties(rs.getString("properties"));
                        messageTraceBean.setQueue(rs.getString("queue"));
                        messageTraceBean.setRoutingKeys(rs.getString("routing_keys"));
                        messageTraceBean.setSuccess(rs.getString("success"));
                        messageTraceBean.setTimestamp(rs.getString("timestamp"));
                        messageTraceBean.setType(rs.getString("type"));
                        messageTraceBean.setUser(rs.getString("user"));
                        messageTraceBean.setVhost(rs.getString("vhost"));

                        messageTraceBeans.add(messageTraceBean);
                    }
                }
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            }
        });
        return messageTraceBeans;
    }

    public int count() {
        AtomicInteger count = new AtomicInteger();
        doWithConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(COUNT)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        count.set(rs.getInt(1));
                    }
                }
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            }
        });
        return count.intValue();
    }
}
