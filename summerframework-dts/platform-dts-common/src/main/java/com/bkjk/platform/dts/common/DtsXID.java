package com.bkjk.platform.dts.common;

public class DtsXID {

    private static int port;

    private static String ipAddress;

    public synchronized static String generateXID(long tranId) {
        return ipAddress + ":" + port + ":" + tranId;
    }

    public static long getTransactionId(String xid) {
        if (xid == null) {
            return -1;
        }
        int idx = xid.lastIndexOf(":");
        return Long.parseLong(xid.substring(idx + 1));
    }

    public static boolean isValidXid(String xid) {
        boolean f = true;
        try {
            DtsXID.getTransactionId(xid);
        } catch (Exception e) {
            f = false;
        }

        return f;
    }

    public static void setIpAddress(String ipAddress) {
        DtsXID.ipAddress = ipAddress;
    }

    public static void setPort(int port) {
        DtsXID.port = port;
    }
}
