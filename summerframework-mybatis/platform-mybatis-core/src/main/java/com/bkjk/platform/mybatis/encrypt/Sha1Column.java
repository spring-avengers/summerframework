package com.bkjk.platform.mybatis.encrypt;

import com.bkjk.platform.mybatis.handler.Sha1ColumnHandler;

public class Sha1Column {
    public static final Sha1Column create(String value) {
        return new Sha1Column(value, true);
    }

    public static final Sha1Column create(String value, boolean addPrefix) {
        return new Sha1Column(value, addPrefix);
    }

    private String value;

    private Sha1Column(String value, boolean addPrefix) {
        if (addPrefix) {
            this.value = Sha1ColumnHandler.PREFIX + value;
        } else {
            this.value = value;
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Sha1Column{" + "value='" + value + '\'' + '}';
    }
}
