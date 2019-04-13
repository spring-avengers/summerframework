package com.bkjk.platform.mybatis.encrypt;

import java.nio.charset.Charset;

import org.springframework.security.crypto.codec.Hex;

public abstract class AbstractEncryptorHolder implements StringEncryptor {

    private String password;
    private String salt;

    public AbstractEncryptorHolder(String password, String salt) {
        this.password = password;
        this.salt = new String(Hex.encode(salt.getBytes(Charset.forName("utf-8"))));
    }

    public String getPassword() {
        return password;
    }

    public String getSalt() {
        return salt;
    }
}
