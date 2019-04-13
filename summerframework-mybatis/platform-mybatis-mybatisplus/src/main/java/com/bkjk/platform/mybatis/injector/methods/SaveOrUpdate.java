package com.bkjk.platform.mybatis.injector.methods;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import com.bkjk.platform.mybatis.injector.MySqlMethod;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import static java.util.stream.Collectors.joining;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/2/21 19:49
 **/
public class SaveOrUpdate extends AbstractMethod {
    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        MySqlMethod sqlMethod = MySqlMethod.SAVE_OR_UPDATE_MYSQL;
        KeyGenerator keyGenerator = new NoKeyGenerator();
        String keyProperty = null;
        String keyColumn = null;
        // 表包含主键处理逻辑,如果不包含主键当普通字段处理
        if (StringUtils.isNotEmpty(tableInfo.getKeyProperty())) {
            if (tableInfo.getIdType() == IdType.AUTO) {
                /** 自增主键 */
                keyGenerator = new Jdbc3KeyGenerator();
                keyProperty = tableInfo.getKeyProperty();
                keyColumn = tableInfo.getKeyColumn();
            } else {
                if (null != tableInfo.getKeySequence()) {
                    keyGenerator = TableInfoHelper.genKeyGenerator(tableInfo, builderAssistant, sqlMethod.getMethod(),
                        languageDriver);
                    keyProperty = tableInfo.getKeyProperty();
                    keyColumn = tableInfo.getKeyColumn();
                }
            }
        }

        String columnScript = SqlScriptUtils
            .convertTrim(SqlScriptUtils.convertIf(keyColumn + ",", String.format("%s != null", keyColumn), true)
                + tableInfo.getAllInsertSqlColumnMaybeIf(), LEFT_BRACKET, RIGHT_BRACKET, null, COMMA);
        String valuesScript = SqlScriptUtils.convertTrim(
            SqlScriptUtils.convertIf("#{" + keyProperty + "},", String.format("%s != null", keyProperty), true)
                + tableInfo.getAllInsertSqlPropertyMaybeIf(null),
            LEFT_BRACKET, RIGHT_BRACKET, null, COMMA);

        StringBuilder updateScript = new StringBuilder();
        updateScript.append("<trim suffixOverrides=\",\">");
        if (DbType.MYSQL == tableInfo.getDbType()) {
            updateScript.append(tableInfo.getFieldList().stream().map(i -> i.getSqlSet("")).collect(joining(NEWLINE)));
        } else {
            throw new UnsupportedOperationException("DbType not supported : " + tableInfo.getDbType());
        }
        updateScript.append("</trim>");
        String sql =
            String.format(sqlMethod.getSql(), tableInfo.getTableName(), columnScript, valuesScript, updateScript);
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        return this.addInsertMappedStatement(mapperClass, modelClass, sqlMethod.getMethod(), sqlSource, keyGenerator,
            keyProperty, keyColumn);
    }

    public static void main(String[] args) {
        System.out.println(SqlScriptUtils.convertIf("id", String.format("%s != null", "id"), false));
        System.out.println(SqlScriptUtils.convertIf("#{id}", String.format("%s != null", "id"), false));
    }
}
