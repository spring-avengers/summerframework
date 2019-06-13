package com.bkjk.platfrom.dts.core.resource.mysql;

import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.bkjk.platfrom.dts.core.resource.mysql.common.SQLType;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo;
import com.google.common.collect.Lists;

public class DbRuntimeContext {

    public static class CommitInfo {

        private TableDataInfo originalValue = new TableDataInfo();

        private TableDataInfo presentValue = new TableDataInfo();

        private String where = "";

        private List<Object> whereParams = Lists.newArrayList();

        private SQLType sqlType = null;

        private String sql = "";

        private List<Object> sqlParams = Lists.newArrayList();

        private String schemaName;

        private String uniqueDbId;

        public TableDataInfo getOriginalValue() {
            return originalValue;
        }

        public TableDataInfo getPresentValue() {
            return presentValue;
        }

        public String getSchemaName() {
            return schemaName;
        }

        public String getSql() {
            return sql;
        }

        public List<Object> getSqlParams() {
            return sqlParams;
        }

        public SQLType getSqlType() {
            return sqlType;
        }

        public String getUniqueDbId() {
            return uniqueDbId;
        }

        public String getWhere() {
            return where;
        }

        public List<Object> getWhereParams() {
            return whereParams;
        }

        public void setOriginalValue(TableDataInfo originalValue) {
            this.originalValue = originalValue;
        }

        public void setPresentValue(TableDataInfo presentValue) {
            this.presentValue = presentValue;
        }

        public void setSchemaName(String schemaName) {
            this.schemaName = schemaName;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public void setSqlParams(List<Object> sqlParams) {
            this.sqlParams = sqlParams;
        }

        public void setSqlType(SQLType sqlType) {
            this.sqlType = sqlType;
        }

        public void setUniqueDbId(String uniqueDbId) {
            this.uniqueDbId = uniqueDbId;
        }

        public void setWhere(String where) {
            this.where = where;
        }

        public void setWhereParams(List<Object> whereParams) {
            this.whereParams = whereParams;
        }

    }

    public static DbRuntimeContext decode(String jsonString) {
        return JSON.parseObject(jsonString, DbRuntimeContext.class);
    }

    public long transId;

    public long branchId;

    private List<CommitInfo> info = Lists.newArrayList();

    public int status;

    public String instanceId;

    public String encode() {
        return JSON.toJSONString(this, SerializerFeature.WriteDateUseDateFormat);
    }

    public long getBranchId() {
        return branchId;
    }

    public List<CommitInfo> getInfo() {
        return info;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public int getStatus() {
        return status;
    }

    public long getTransId() {
        return transId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public void setInfo(List<CommitInfo> info) {
        this.info = info;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setTransId(long transId) {
        this.transId = transId;
    }
}
