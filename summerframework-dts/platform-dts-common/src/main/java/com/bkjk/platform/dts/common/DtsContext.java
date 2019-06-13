package com.bkjk.platform.dts.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public abstract class DtsContext {

    protected static final String TXC_XID_KEY = "XID";

    private static final List<DtsContext> DTS_CONTEXTS = new ArrayList<DtsContext>();

    static {
        ServiceLoader<DtsContext> dtsContexts = ServiceLoader.load(DtsContext.class);
        Iterator<DtsContext> it = dtsContexts.iterator();
        while (it.hasNext()) {
            DTS_CONTEXTS.add(it.next());
        }
    }

    public static DtsContext getInstance() {
        if (DTS_CONTEXTS == null && DTS_CONTEXTS.size() == 0) {
            throw new UnsupportedOperationException(
                "Please choose one microservice framework, Dts only support saluki or spring cloud");
        }
        return DTS_CONTEXTS.get(0);
    }

    public abstract void bind(String xid);

    public abstract String getCurrentXid();

    public abstract boolean inTransaction();

    public abstract int priority();

    public abstract void unbind();

}
