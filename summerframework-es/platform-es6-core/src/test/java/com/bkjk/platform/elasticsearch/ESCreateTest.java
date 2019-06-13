package com.bkjk.platform.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.bkjk.platform.elasticsearch.domain.Sku;
import com.bkjk.platform.elasticsearch.domain.Spu;
import com.bkjk.platform.elasticsearch.support.ElasticsearchTemplate;
import com.bkjk.platform.elasticsearch.wrapper.ESBasicInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class ESCreateTest {
    @Resource
    private ElasticsearchTemplate elasticsearchTemplate;
    private XContentBuilder mapping;
    private Spu spu1;
    private Spu spu2;
    private ESBasicInfo esBasicInfo;

    @After
    public void clear() {
        spu1 = null;
        spu2 = null;

        mapping = null;
        esBasicInfo = null;
    }

    @Test
    public void createIndex() {
        if (!elasticsearchTemplate.isExistedIndex("es_test")) {
            assertThat(elasticsearchTemplate.createIndex("es_test"), is(true));
        }

        assertThat(elasticsearchTemplate.isExistedIndex("es_test"), is(true));
    }

    @Test
    public void createMapping() throws IOException {
        assertThat(elasticsearchTemplate.createMapping("es_test", "type", mapping), is(true));
    }

    @Test
    public void deleteIndex() {
        if (elasticsearchTemplate.isExistedIndex("es_test")) {
            assertThat(elasticsearchTemplate.deleteIndex("es_test"), is(true));
        }

        assertThat(elasticsearchTemplate.isExistedIndex("es_test"), is(false));
    }

    @Test
    public void getAllMapping() throws IOException {
        List<Map<String, Object>> result = elasticsearchTemplate.getAllMapping("es_test");
        log.info("result is:{}", result);
        assertThat(result.size(), is(1));
    }

    @Test
    public void getMapping() throws IOException {
        String result = elasticsearchTemplate.getMapping("es_test", "type");
        log.info("result is:{}", result);
        assertThat(result, containsString("\"ignore_above\":256"));
    }

    @Before
    public void init() throws IOException {
        spu1 = new Spu();
        spu1.setProductCode("7b28c293-4d06-4893-aad7-e4b6ed72c260");
        spu1.setProductName("android手机");
        spu1.setBrandCode("H-001");
        spu1.setBrandName("华为Nexus");
        spu1.setCategoryCode("C-001");
        spu1.setCategoryName("手机");

        Sku sku1 = new Sku();
        sku1.setSkuCode("001");
        sku1.setSkuName("华为Nexus P6");
        sku1.setSkuPrice(4000);
        sku1.setColor("Red");

        Sku sku2 = new Sku();
        sku2.setSkuCode("002");
        sku2.setSkuName("华为 P8");
        sku2.setSkuPrice(3000);
        sku2.setColor("Blank");

        Sku sku3 = new Sku();
        sku3.setSkuCode("003");
        sku3.setSkuName("华为Nexus P6下一代");
        sku3.setSkuPrice(5000);
        sku3.setColor("White");

        spu1.getSkus().add(sku1);
        spu1.getSkus().add(sku2);
        spu1.getSkus().add(sku3);

        spu2 = new Spu();
        spu2.setProductCode("AVYmdpQ_cnzgjoSZ6ent");
        spu2.setProductName("运动服装");
        spu2.setBrandCode("YD-001");
        spu2.setBrandName("李宁");
        spu2.setCategoryCode("YDC-001");
        spu2.setCategoryName("服装");

        Sku sku21 = new Sku("YD001", "李宁衣服1", "Green", "2XL", 4000);
        Sku sku22 = new Sku("YD002", "李宁衣服2", "Green", "L", 3000);
        Sku sku23 = new Sku("YD003", "李宁衣服3", "Green", "M", 5000);

        spu2.getSkus().add(sku21);
        spu2.getSkus().add(sku22);
        spu2.getSkus().add(sku23);

        mapping = jsonBuilder().prettyPrint().startObject().startObject("properties").startObject("productCode")
            .field("type", "text").field("index", "not_analyzed").endObject().startObject("productName")
            .field("type", "text").field("index", "not_analyzed").endObject().startObject("brandCode")
            .field("type", "text").field("index", "not_analyzed").endObject().startObject("brandName")
            .field("type", "text").field("index", "not_analyzed").endObject().startObject("categoryCode")
            .field("type", "text").field("index", "not_analyzed").endObject().startObject("categoryName")
            .field("type", "text").field("index", "not_analyzed").endObject().startObject("imageTag")
            .field("type", "text").field("index", "not_analyzed").endObject().startObject("skus")
            .startObject("properties").startObject("skuCode").field("type", "text").field("index", "not_analyzed")
            .endObject().startObject("skuName").field("type", "text").field("index", "not_analyzed").endObject()
            .startObject("color").field("type", "text").field("index", "not_analyzed").endObject().startObject("size")
            .field("type", "text").field("index", "not_analyzed").endObject().startObject("skuPrice")
            .field("type", "long").field("index", "not_analyzed").endObject().endObject().endObject().endObject()
            .endObject();

        esBasicInfo = new ESBasicInfo();
        esBasicInfo.setIndex("es_test");
        esBasicInfo.setType("type");
        esBasicInfo.setIds(new String[] {"1"});
    }

    @Test
    public void operateBatchData() throws IOException {
        String[] arrayIDs = {"1", "2"};
        esBasicInfo.setIds(arrayIDs);
        assertThat(elasticsearchTemplate.addBatchData(esBasicInfo, spu1), is(2));

        assertThat(elasticsearchTemplate.updateBatchData(esBasicInfo, spu1), is(2));

        assertThat(elasticsearchTemplate.deleteBatchData(esBasicInfo), is(2));

        arrayIDs = new String[] {"1", "2", "3"};
        esBasicInfo.setIds(arrayIDs);
        assertThat(elasticsearchTemplate.addBatchData(esBasicInfo, spu2), is(3));

        assertThat(elasticsearchTemplate.updateBatchData(esBasicInfo, spu2), is(3));

        assertThat(elasticsearchTemplate.deleteBatchData(esBasicInfo), is(3));
    }

    @Test
    public void operateData() throws IOException {
        assertThat(elasticsearchTemplate.addData(esBasicInfo, spu1), is(true));

        assertThat(elasticsearchTemplate.updateData(esBasicInfo, spu2), is(true));

        assertThat(elasticsearchTemplate.deleteData(esBasicInfo), is(true));
    }
}
