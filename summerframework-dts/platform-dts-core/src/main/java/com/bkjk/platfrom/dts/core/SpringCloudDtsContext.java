package com.bkjk.platfrom.dts.core;

import java.util.HashMap;
import java.util.Map;

import com.bkjk.platform.dts.common.DtsContext;

public class SpringCloudDtsContext extends DtsContext {
    private static final ThreadLocal<SpringCloudDtsContext> LOCAL =
        new InheritableThreadLocal<SpringCloudDtsContext>() {

            @Override
            protected SpringCloudDtsContext initialValue() {
                return new SpringCloudDtsContext();
            }
        };

    public static SpringCloudDtsContext getContext() {
        return LOCAL.get();
    }

    public static void removeContext() {
        LOCAL.remove();
    }

    private final Map<String, String> attachments = new HashMap<String, String>();

    @Override
    public void bind(String xid) {
        SpringCloudDtsContext.getContext().setAttachment(TXC_XID_KEY, xid);
    }

    public String getAttachment(String key) {
        return attachments.get(key);
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    @Override
    public String getCurrentXid() {
        return SpringCloudDtsContext.getContext().getAttachment(TXC_XID_KEY);
    }

    @Override
    public boolean inTransaction() {
        return getCurrentXid() != null;
    }

    @Override
    public int priority() {
        return 0;
    }

    public SpringCloudDtsContext removeAttachment(String key) {
        attachments.remove(key);
        return this;
    }

    public SpringCloudDtsContext setAttachment(Map<String, String> value) {
        if (value != null && value.isEmpty()) {
            attachments.putAll(value);
        }
        return this;
    }

    public SpringCloudDtsContext setAttachment(String key, String value) {
        if (value == null) {
            attachments.remove(key);
        } else {
            attachments.put(key, value);
        }
        return this;
    }

    @Override
    public void unbind() {
        SpringCloudDtsContext.removeContext();
    }
}
