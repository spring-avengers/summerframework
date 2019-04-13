package com.bkjk.platform.mybatis.encrypt;

public interface StringEncryptor {

    String decrypt(String encryptedText);

    String encrypt(String text);

}
