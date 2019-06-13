package com.bkjk.platfrom.dts.core.resource.mysql.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

public class BlobUtils {
    public static String blob2string(Blob blob) throws SQLException {
        if (blob == null) {
            return null;
        }
        return new String(blob.getBytes(1, (int)blob.length()));

    }

    public static String inputStream2String(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = -1;
        while ((i = is.read()) != -1) {
            baos.write(i);
        }
        return baos.toString();
    }

    public static Blob string2blob(String str) throws SerialException, SQLException {
        if (str == null) {
            return null;
        }
        return new SerialBlob(str.getBytes());

    }

}
