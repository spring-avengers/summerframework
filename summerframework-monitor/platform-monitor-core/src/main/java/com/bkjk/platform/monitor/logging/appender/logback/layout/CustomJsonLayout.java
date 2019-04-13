package com.bkjk.platform.monitor.logging.appender.logback.layout;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import ch.qos.logback.core.util.ContextUtil;
import lombok.Setter;

@Setter
public class CustomJsonLayout extends JsonLayout {
    public static final String LINE_NUMBER = "line_number";
    public static final String CLASS_NAME = "class_name";
    public static final String METHOD_NAME = "method_name";
    public static final String FILE_NAME = "file_name";
    public static final String HOST = "host";
    private final String HOST_NAME = getHostName();

    private boolean includeLineNumber = true;
    private boolean includeClassName = true;
    private boolean includeMethodName = true;
    private boolean includeHost = true;
    private boolean includeFileName = true;
    private List<String> additionalFields = new ArrayList<>();

    public void addAdditionalField(String additionalField) {
        additionalFields.add(additionalField);
    }

    @Override
    protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
        add(HOST, includeHost, HOST_NAME, map);
        if (event.hasCallerData()) {
            StackTraceElement callerData = event.getCallerData()[0];
            addLine(LINE_NUMBER, includeLineNumber, callerData.getLineNumber(), map);
            add(CLASS_NAME, includeClassName, callerData.getClassName(), map);
            add(METHOD_NAME, includeMethodName, callerData.getMethodName(), map);
            add(FILE_NAME, includeFileName, callerData.getFileName(), map);
        }
        if (additionalFields != null) {
            additionalFields.forEach(field -> {
                String[] p = field.split("\\|");
                if (p.length == 2) {
                    add(p[0], true, p[1], map);
                } else {
                    addWarn("Unable to parse property string: " + field);
                }
            });
        }
    }

    private void addLine(String fieldName, boolean field, int value, Map<String, Object> map) {
        if (field) {
            map.put(fieldName, value);
        }
    }

    private String getHostName() {
        try {
            return ContextUtil.getLocalHostName();
        } catch (UnknownHostException | SocketException e) {
            return "undefined";
        }
    }
}
