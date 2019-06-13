package com.bkjk.platfrom.dts.core.resource.mysql.common;

import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine.TxcField;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Set;

public class ResultConvertUtils {

    public static List<TxcLine> convertWithPrimary(ResultSet resultSet, Set<String> primaryKeyNameSet, SQLType sqlType)
        throws SQLException {
        List<TxcLine> txcLines = Lists.newArrayList();
        ResultSetMetaData metaData = resultSet.getMetaData();
        while (resultSet.next()) {
            TxcLine txcLine = new TxcLine();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                if (primaryKeyNameSet.contains(metaData.getColumnName(i))
                    && (sqlType == SQLType.UPDATE || sqlType == SQLType.SELECT || sqlType == SQLType.DELETE)) {
                    txcLine.getPrimaryKeyValues().add(
                        PkPair.of(metaData.getColumnName(i), getDataByType(i, metaData.getColumnType(i), resultSet)));
                } else {
                    TxcField txcField = new TxcField();
                    txcField.setName(metaData.getColumnName(i));
                    txcField.setType(metaData.getColumnType(i));
                    txcField.setValue(getDataByType(i, metaData.getColumnType(i), resultSet));
                    txcField.setJdkValue(SerializeUtils.serialize(txcField.getValue()));
                    txcLine.getFields().add(txcField);
                }
            }
            txcLines.add(txcLine);
        }
        return txcLines;
    }

    private static Object getDataByType(int index, int columnType, ResultSet resultSet) throws SQLException {
        if (columnType == Types.BIT) {
            return resultSet.getByte(index);
        }
        if (columnType == Types.TINYINT) {
            return resultSet.getByte(index);
        }
        if (columnType == Types.SMALLINT) {
            return resultSet.getShort(index);
        }
        if (columnType == Types.INTEGER) {
            return resultSet.getInt(index);
        }
        if (columnType == Types.BIGINT) {
            return resultSet.getLong(index);
        }
        if (columnType == Types.FLOAT) {
            return resultSet.getFloat(index);
        }
        if (columnType == Types.DOUBLE) {
            return resultSet.getDouble(index);
        }
        if (columnType == Types.NUMERIC) {
            return resultSet.getInt(index);
        }
        if (columnType == Types.DECIMAL) {
            return resultSet.getBigDecimal(index);
        }
        if (columnType == Types.CHAR) {
            return resultSet.getString(index);
        }
        if (columnType == Types.VARCHAR) {
            return resultSet.getString(index);
        }
        if (columnType == Types.LONGNVARCHAR) {
            return resultSet.getString(index);
        }
        if (columnType == Types.DATE) {
            return resultSet.getDate(index);
        }
        if (columnType == Types.TIME) {
            return resultSet.getTime(index);
        }
        if (columnType == Types.NCHAR) {
            return resultSet.getNString(index);
        }
        if (columnType == Types.NVARCHAR) {
            return resultSet.getNString(index);
        }
        if (columnType == Types.OTHER) {
            return resultSet.getObject(index);
        }
        if (columnType == Types.BLOB) {
            return resultSet.getBlob(index);
        }
        if (columnType == Types.BOOLEAN) {
            return resultSet.getBoolean(index);
        }
        if (columnType == Types.ARRAY) {
            return resultSet.getArray(index);
        }
        if (columnType == Types.TIMESTAMP) {
            return resultSet.getTimestamp(index);
        }
        return resultSet.getObject(index);
    }
}
