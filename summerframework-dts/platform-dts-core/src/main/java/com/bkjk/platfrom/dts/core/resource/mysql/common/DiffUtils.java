package com.bkjk.platfrom.dts.core.resource.mysql.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class DiffUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException, JsonProcessingException {
                gen.writeString(value.setScale(2).toString());
            }
        });

        simpleModule.addSerializer(StringReader.class, new JsonSerializer<StringReader>() {
            @Override
            public void serialize(StringReader value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException, JsonProcessingException {
                gen.writeString(read(value));
            }
        });

        objectMapper.registerModule(simpleModule);
    }

    public static boolean diff(Object oldDifDto, Object curDifDto) {
        try {
            String old = objectMapper.writeValueAsString(oldDifDto);
            curDifDto = JSON.parseObject(JSON.toJSONString(curDifDto, SerializerFeature.WriteDateUseDateFormat),
                curDifDto.getClass());
            String cur = objectMapper.writeValueAsString(curDifDto);
            JsonNode oldJsonNode = objectMapper.readTree(old);
            JsonNode curJsonNode = objectMapper.readTree(cur);

            if (oldJsonNode.equals(curJsonNode)) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }

    }

    private static final Comparator<TableDataInfo.TxcLine.TxcField> TXC_FIELD_COMPARATOR =
        Comparator.comparing(TableDataInfo.TxcLine.TxcField::getName);

    public static boolean diff(TableDataInfo.TxcLine oldTxcLine, TableDataInfo.TxcLine curTxcLine) {
        if (Objects.equals(oldTxcLine, curTxcLine)) {
            return true;
        } else if (Objects.isNull(oldTxcLine) || Objects.isNull(curTxcLine)) {
            return false;
        } else {
            List<TableDataInfo.TxcLine.TxcField> oldFields = Lists.newArrayList(oldTxcLine.getFields());
            Collections.sort(oldFields, TXC_FIELD_COMPARATOR);
            List<TableDataInfo.TxcLine.TxcField> curFields = Lists.newArrayList(curTxcLine.getFields());
            Collections.sort(curFields, TXC_FIELD_COMPARATOR);
            for (TableDataInfo.TxcLine.TxcField oldField : oldFields) {
                TableDataInfo.TxcLine.TxcField curField = curFields.get(oldFields.indexOf(oldField));
                if (!Objects.equals(oldField.getName(), curField.getName())) {
                    return false;
                } else if (!Objects.equals(oldField.getValue(), curField.getValue())) {
                    if (Objects.isNull(oldField.getValue()) || Objects.isNull(curField.getValue())) {
                        return false;
                    }
                    if (oldField.getValue() instanceof Number && curField.getValue() instanceof Number) {
                        if (new BigDecimal(String.valueOf(oldField.getValue()))
                            .compareTo(new BigDecimal(String.valueOf(curField.getValue()))) != 0) {
                            return false;
                        }
                    } else {
                        try {
                            Object oldSerializedValue = SerializeUtils.derialize(oldField.getJdkValue());
                            Object curSerializedValue = SerializeUtils.derialize(curField.getJdkValue());
                            if (!Objects.equals(oldSerializedValue, curSerializedValue)) {
                                if (oldSerializedValue instanceof Number && curSerializedValue instanceof Number) {
                                    if (new BigDecimal(String.valueOf(oldSerializedValue))
                                        .compareTo(new BigDecimal(String.valueOf(curSerializedValue))) != 0) {
                                        return false;
                                    }
                                } else {
                                    return false;
                                }
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static void main(String[] args) {

        StringReader test = new StringReader(
            "a:2:{s:20:\"php_serialize_option\";s:1:\" \";s:9:\"orderdata\";a:1:{i:0;a:22:{s:11:\"refund_type\";s:1:\"0\";s:8:\"dateline\";s:1:\"0\";s:11:\"mk_order_id\";s:4:\"null\";s:5:\"stype\";s:2:\"16\";s:11:\"sl_nickname\";s:10:\"zhigb_0016\";s:3:\"num\";s:1:\"1\";s:5:\"ptype\";s:2:\"23\";s:5:\"title\";s:55:\"lqq田酞递址秽梳猜欣勾翅#169779075稿件中标\";s:10:\"sl_user_id\";s:8:\"19182259\";s:7:\"link_id\";s:1:\"0\";s:11:\"offer_price\";s:3:\"0.0\";s:5:\"mtype\";s:1:\"0\";s:12:\"product_pkid\";s:9:\"169779075\";s:7:\"data_id\";s:8:\"90567713\";s:7:\"user_id\";s:8:\"19182244\";s:5:\"price\";s:4:\"50.0\";s:11:\"refund_time\";s:1:\"0\";s:8:\"nickname\";s:18:\"靖哥哥的店铺\";s:13:\"refund_amount\";s:3:\"0.0\";s:8:\"order_id\";s:8:\"90567198\";s:8:\"at_price\";s:4:\"50.0\";s:12:\"refund_state\";s:1:\"0\";}}}");
        String read = read(test);

        System.out.println(read);
    }

    public static String read(StringReader stringReader) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            stringReader.reset();
            int c;
            while ((c = stringReader.read()) != -1) {
                stringBuilder.append((char)c);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            return "";
        }
    }

}
