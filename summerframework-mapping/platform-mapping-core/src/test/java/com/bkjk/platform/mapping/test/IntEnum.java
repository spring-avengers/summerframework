package com.bkjk.platform.mapping.test;

public enum IntEnum {
    SUCCESS(200, "成功"), ERROR(400, "服务异常"), PARAMETER_ERR(300, "参数错误"), PARAMETER_ERR_PASS_PHONE(301, "与手机号相同"),
    PARAMETER_ERR_PASS_CERTNO(302, "与身份证号相同"), ERR_TOO_OFTEN(303, "请求过于频繁");

    private Integer code;

    private String info;

    IntEnum(Integer code, String info) {
        this.code = code;
        this.info = info;
    }

    public Integer getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }
}
