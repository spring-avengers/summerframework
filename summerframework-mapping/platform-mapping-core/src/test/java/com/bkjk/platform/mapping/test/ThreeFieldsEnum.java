package com.bkjk.platform.mapping.test;

public enum ThreeFieldsEnum {
    QUICKPAY("001", "FASTPAY", "快捷支付"), WECHATPAY("002", "WECHATPAY", "微信支付"), BALANCEPAY("003", "EHOMEPAY", "钱包余额支付"),
    NO_VIP_QUICKPAY("004", "NO_VIP_QUICKPAY", "非会员快捷支付");

    private String code;
    private String key;
    private String name;

    ThreeFieldsEnum(String code, String key, String name) {
        this.code = code;
        this.name = name;
        this.key = key;
    }

    public String getCode() {
        return code;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

}
