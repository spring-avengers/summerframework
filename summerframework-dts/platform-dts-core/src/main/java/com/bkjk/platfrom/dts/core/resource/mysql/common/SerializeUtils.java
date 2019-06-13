package com.bkjk.platfrom.dts.core.resource.mysql.common;

import java.io.*;
import java.sql.SQLException;

public class SerializeUtils {
    public static byte[] serialize(Object obj) throws SQLException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
        } catch (IOException e) {
            throw new SQLException(String.format("Serialize %s failed", obj), e);
        }
        return bos.toByteArray();
    }

    public static Object derialize(byte[] bytes) throws SQLException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bis);
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SQLException("Deserialization failed", e);
        }
    }
}
