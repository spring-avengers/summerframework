package com.bkjk.platform.mybatis.injector.methods;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.bkjk.platform.mybatis.injector.MySqlMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;

import java.util.List;

public class UpdateBatchById extends AbstractMethod {

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo table) {
        StringBuilder set = new StringBuilder();
        set.append("<trim prefix=\"SET\" suffixOverrides=\",\">\n");
        MySqlMethod sqlMethod = MySqlMethod.UPDATE_BATCH_BY_ID_MYSQL;
        if (DbType.ORACLE == table.getDbType()) {
            sqlMethod = MySqlMethod.UPDATE_BATCH_BY_ID_ORACLE;
            List<TableFieldInfo> fieldList = table.getFieldList();
            for (TableFieldInfo fieldInfo : fieldList) {
                set.append(fieldInfo.getColumn()).append("=#{item.").append(fieldInfo.getEl()).append("},");
            }
        } else if (DbType.MYSQL == table.getDbType()) {
            List<TableFieldInfo> fieldList = table.getFieldList();
            for (TableFieldInfo fieldInfo : fieldList) {
                set.append("\n<trim prefix=\"").append(fieldInfo.getColumn()).append("=CASE ");
                set.append(table.getKeyColumn()).append("\" suffix=\"END,\">");
                set.append("\n<foreach collection=\"list\" item=\"i\" index=\"index\">");
                set.append(convertIfTag(fieldInfo, "i.", false));
                set.append("\nWHEN ").append("#{i.").append(table.getKeyProperty());
                set.append("} THEN #{i.").append(fieldInfo.getEl()).append("}");
                set.append(convertIfTag(fieldInfo, true));
                set.append("\n</foreach>");
                set.append("\n</trim>");
            }
        } else {
            throw new UnsupportedOperationException("DbType not supported" + table.getDbType());
        }
        set.append("\n</trim>");
        String sql = String.format(sqlMethod.getSql(), table.getTableName(), set.toString(), table.getKeyColumn(),
            table.getKeyProperty());
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        return this.addUpdateMappedStatement(mapperClass, modelClass, sqlMethod.getMethod(), sqlSource);
    }

    protected String convertIfTag(SqlCommandType sqlCommandType, TableFieldInfo fieldInfo, String prefix,
        boolean colse) {
        /* 前缀处理 */
        String property = fieldInfo.getProperty();
        if (null != prefix) {
            property = prefix + property;
        }
        /* 判断策略 */
        if (sqlCommandType == SqlCommandType.INSERT && fieldInfo.getFieldStrategy() == FieldStrategy.DEFAULT) {
            return "";
        }
        if (fieldInfo.getFieldStrategy() == FieldStrategy.IGNORED) {
            return "";
        } else if (fieldInfo.getFieldStrategy() == FieldStrategy.NOT_EMPTY) {
            if (colse) {
                return "</if>";
            } else {
                return String.format("\n\t<if test=\"%s!=null and %s!=''\">", property, property);
            }
        } else {
            // FieldStrategy.NOT_NULL
            if (colse) {
                return "</if>";
            } else {
                return String.format("\n\t<if test=\"%s!=null\">", property);
            }
        }
    }

    protected String convertIfTag(TableFieldInfo fieldInfo, String prefix, boolean colse) {
        return convertIfTag(SqlCommandType.UNKNOWN, fieldInfo, prefix, colse);
    }

    protected String convertIfTag(TableFieldInfo fieldInfo, boolean colse) {
        return convertIfTag(fieldInfo, null, colse);
    }
}
