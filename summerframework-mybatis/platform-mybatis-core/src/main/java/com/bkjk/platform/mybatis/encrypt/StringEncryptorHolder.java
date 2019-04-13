package com.bkjk.platform.mybatis.encrypt;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.nio.charset.Charset;

import static com.bkjk.platform.mybatis.PlatformMybatisApplicationRunListener.MYBATIS_ENCRYPT_PASSWORD;
import static com.bkjk.platform.mybatis.PlatformMybatisApplicationRunListener.MYBATIS_ENCRYPT_SALT;

public class StringEncryptorHolder {

    public static final Logger logger = LoggerFactory.getLogger(StringEncryptorHolder.class);

    private static StringEncryptor stringEncryptor;

    public static StringEncryptor getStringEncryptor() {
        if (stringEncryptor == null) {
            init();
        }
        return stringEncryptor;
    }

    private static void init() {
        synchronized (StringEncryptorHolder.class) {
            if (System.getProperty(MYBATIS_ENCRYPT_PASSWORD) == null
                || System.getProperty(MYBATIS_ENCRYPT_SALT) == null) {
                logger.error(
                    "Cant not find StringEncryptor. If you want to use EncryptedColumn, you must set you own password and salt.");
                logger.error("For example: mybatis.encrypt.password=yourpassword mybatis.encrypt.salt=yoursalt");
                throw new RuntimeException("Cant not find StringEncryptor.");
            }
            setStringEncryptor(new StrongStringEncryptor(System.getProperty(MYBATIS_ENCRYPT_PASSWORD),
                System.getProperty(MYBATIS_ENCRYPT_SALT)));
        }
    }

    public static void main(String[] args) {

        TextEncryptor encryptor =
            Encryptors.delux("pass", new String(Hex.encode("salt".getBytes(Charset.forName("utf-8")))));
        System.out.println(encryptor.encrypt("sadfsadfasfsadf"));
        System.out.println(encryptor.encrypt("sadfsadfasfsadf"));
        System.out.println(encryptor.decrypt(encryptor.encrypt("这是密码")));
    }

    public static void setStringEncryptor(StringEncryptor stringEncryptor) {
        if (StringEncryptorHolder.stringEncryptor != null) {
            logger.error("StringEncryptor cant not be set twice! Ignored {}", stringEncryptor);
            return;
        }
        StringEncryptorHolder.stringEncryptor = stringEncryptor;
    }
}
