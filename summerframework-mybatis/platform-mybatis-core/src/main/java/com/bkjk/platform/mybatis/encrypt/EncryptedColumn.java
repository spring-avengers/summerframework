package com.bkjk.platform.mybatis.encrypt;

public class EncryptedColumn {
    public static final EncryptedColumn create(String value) {
        return new EncryptedColumn(value);
    }

    private String value;

    private EncryptedColumn(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "EncryptedColumn{" + "value='" + value + '\'' + '}';
    }
}
