package com.bkjk.platfrom.dts.core.resource.mysql.common;

import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TableMetaInfo {

    public static class ColumnInfo {

        private String tableName;

        private String columnName;

        private int type;

        private int keyType;

        private boolean isAllowNull;

        private String defaultValue;

        private String extra;

        public String getColumnName() {
            return columnName;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getExtra() {
            return extra;
        }

        public int getKeyType() {
            return keyType;
        }

        public String getTableName() {
            return tableName;
        }

        public int getType() {
            return type;
        }

        public boolean isAllowNull() {
            return isAllowNull;
        }

        public void setAllowNull(boolean allowNull) {
            isAllowNull = allowNull;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public void setExtra(String extra) {
            this.extra = extra;
        }

        public void setKeyType(int keyType) {
            this.keyType = keyType;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public void setType(int type) {
            this.type = type;
        }
    }

    private String schemaName;

    private String tableName;

    private Map<String, ColumnInfo> columnInfoMap = Maps.newHashMap();

    public String getAutoIncrementPrimaryKey() {
        for (Map.Entry<String, ColumnInfo> entry : this.columnInfoMap.entrySet()) {
            ColumnInfo columnInfo = entry.getValue();
            if (columnInfo.getKeyType() == 0 && columnInfo.getExtra().equals("auto_increment")) {
                return entry.getKey();
            }
        }
        return null;
    }

    public ColumnInfo getColumnByName(String name) {
        String str = name.toUpperCase();
        ColumnInfo ret = this.columnInfoMap.get(str);
        if (ret == null) {
            if (name.charAt(0) == '`') {
                ret = this.columnInfoMap.get(str.substring(1, name.length() - 1));
            } else {
                ret = this.columnInfoMap.get("`" + str + "`");
            }
        }
        return ret;
    }

    public Map<String, ColumnInfo> getColumnInfoMap() {
        return columnInfoMap;
    }

    public Map<String, ColumnInfo> getPrimaryKey() {
        HashMap<String, ColumnInfo> ret = new HashMap<>();
        for (Map.Entry<String, ColumnInfo> entry : this.columnInfoMap.entrySet()) {
            ColumnInfo columnInfo = entry.getValue();
            if (columnInfo.getKeyType() == 0) {
                ret.put(entry.getKey(), columnInfo);
            }
        }
        return ret;
    }

    public Set<String> getPrimaryKeyName() {
        Map<String, ColumnInfo> primaryKey = getPrimaryKey();
        return primaryKey.keySet();
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setColumnInfoMap(Map<String, ColumnInfo> columnInfoMap) {
        this.columnInfoMap = columnInfoMap;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
