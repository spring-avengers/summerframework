package com.bkjk.platform.dts.common;

public class DtsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private int result;

    public DtsException() {
    }

    public DtsException(int result, String msg) {
        super(msg);
        this.setResult(result);
    }

    public DtsException(String msg) {
        this(1, msg);
    }

    public DtsException(Throwable th) {
        super(th);
        this.result = 0;
    }

    public DtsException(Throwable th, String msg) {
        super(msg, th);
        this.result = 0;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }
}
