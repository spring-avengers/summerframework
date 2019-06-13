package com.bkjk.platform.mapping.test;

public enum StringEnum {

    ING("01", "进行中"), SUCC("02", "订单成功"), FAIL("03", "失败"), ACCEPTED("04", "受理成功"), REFUND_SUCC("05", "退款成功");

    private final String code;

    private final String info;

    StringEnum(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }
}
