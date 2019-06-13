package com.bkjk.platfrom.dts.core.resource.mysql.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.stream.Collectors;

public class TableDataInfo {

    public static class TxcLine {

        public static class TxcField {

            private String name;

            @JsonIgnore
            private int type;

            private Object value;

            private byte[] jdkValue;

            public String getName() {
                return name;
            }

            @JsonIgnore
            public String getSqlName() {
                return "`" + name + "`";
            }

            public int getType() {
                return type;
            }

            public void setType(int type) {
                this.type = type;
            }

            public void setName(String name) {
                this.name = name;
            }

            public Object getValue() {
                return value;
            }

            public void setValue(Object value) {
                this.value = value;
            }

            public byte[] getJdkValue() {
                return jdkValue;
            }

            public void setJdkValue(byte[] jdkValue) {
                this.jdkValue = jdkValue;
            }
        }

        private List<TxcField> fields = Lists.newArrayList();

        private List<PkPair<String, Object>> primaryKeyValues = Lists.newArrayList();

        public List<TxcField> getFields() {
            return fields;
        }

        public void setFields(List<TxcField> fields) {
            this.fields = fields;
        }

        public List<PkPair<String, Object>> getPrimaryKeyValues() {
            return primaryKeyValues;
        }

        public void setPrimaryKeyValues(List<PkPair<String, Object>> primaryKeyValues) {
            this.primaryKeyValues = primaryKeyValues;
        }

        @JsonIgnore
        public String getPkCondition() {
            return getPrimaryKeyValues().stream().map((primaryKeyValue -> primaryKeyValue.getKey() + " = ?"))
                .collect(Collectors.joining(" and "));
        }
    }

    private String schemaName;

    private String tableName;

    private String alias;

    private List<TxcLine> line = Lists.newArrayList();

    public String getAlias() {
        return alias;
    }

    public List<TxcLine> getLine() {
        return line;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setLine(List<TxcLine> line) {
        this.line = line;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

}
