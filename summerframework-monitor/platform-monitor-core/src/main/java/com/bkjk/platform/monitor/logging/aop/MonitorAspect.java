package com.bkjk.platform.monitor.logging.aop;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class MonitorAspect {
    protected Set<String> paramBlacklist = new HashSet<>(Arrays.asList("password", "passwd", "secret", "authorization",
        "api_key", "apikey", "access_token", "accesstoken"));

    protected String scrubbedValue = "xxxxx";

    protected boolean enableDataScrubbing = true;

    protected Pattern paramBlacklistRegex;

    public void setCustomParamBlacklist(Set<String> customParamBlacklist) {
        customParamBlacklist.forEach(i -> paramBlacklist.add(i.toLowerCase()));
    }

    public void setDefaultScrubbedValue(String defaultScrubbedValue) {
        scrubbedValue = defaultScrubbedValue;
    }

    public void setEnableDataScrubbing(boolean enableDataScrubbing) {
        this.enableDataScrubbing = enableDataScrubbing;
    }

    public void setParamBlacklistRegex(String paramBlacklistRegex) {
        this.paramBlacklistRegex = Pattern.compile(paramBlacklistRegex);
    }
}
