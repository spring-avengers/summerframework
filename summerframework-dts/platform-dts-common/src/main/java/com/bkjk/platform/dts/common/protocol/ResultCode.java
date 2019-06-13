package com.bkjk.platform.dts.common.protocol;

public enum ResultCode {

    OK(1),

    ERROR(0);

    private int value;

    private ResultCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
