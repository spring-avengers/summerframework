package com.bkjk.platform.monitor;

import java.io.Serializable;

import com.bkjk.platform.monitor.util.JsonUtil;

final class LogEvent implements Serializable {

    private static final long serialVersionUID = 3416383825972052199L;

    private final String type;

    private final Object param;

    public LogEvent(String type, Object param) {
        super();
        this.type = type;
        this.param = param;
    }

    public String getParam() {
        return JsonUtil.toJson(this.param);

    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
