package com.bkjk.platform.mybatis.encrypt;

import org.apache.commons.codec.digest.DigestUtils;

public class Sha1HexUtil {
    public static String sha1Hex(String text) {
        return DigestUtils.sha1Hex(text);
    }
}
