package com.bkjk.platform.mybatis.encrypt;

import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

public class StandardStringEncryptor extends AbstractEncryptorHolder {

    private final TextEncryptor encryptor;

    public StandardStringEncryptor(String password, String salt) {
        super(password, salt);
        encryptor = Encryptors.text(super.getPassword(), super.getSalt());
    }

    @Override
    public String decrypt(String encryptedText) {
        return encryptor.decrypt(encryptedText);
    }

    @Override
    public String encrypt(String text) {
        return encryptor.encrypt(text);
    }
}
