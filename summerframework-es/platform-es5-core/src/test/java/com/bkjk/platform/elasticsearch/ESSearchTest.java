package com.bkjk.platform.elasticsearch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SpanQueryBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
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
import com.bkjk.platform.elasticsearch.wrapper.HighLight;
import com.bkjk.platform.elasticsearch.wrapper.QueryCondition;
import com.bkjk.platform.elasticsearch.wrapper.QueryPair;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class ESSearchTest {
    @Resource
    private ElasticsearchTemplate elasticsearchTemplate;
    @Resource
    private TransportClient esClient;
    private Spu spu1;
    private Spu spu2;
    private Spu spu3;
    private ESBasicInfo esBasicInfo;
    private ObjectMapper mapper = new ObjectMapper();
    private QueryCondition queryCondition;
    private QueryPair queryPair;

    @Test
    public void boolQuery() throws IOException {
        queryPair.setFieldNames(new String[] {"productName"});
        queryPair.setContent("android");
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery(queryPair.getFieldNames()[0], queryPair.getContent()));
        queryCondition.setQueryBuilder(queryBuilder);

        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));

        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu1));
    }

    @After
    public void clear() {
        esBasicInfo.setIndex("es_test");
        esBasicInfo.setType("type");
        esBasicInfo.setIds(new String[] {"1", "2", "3"});

        elasticsearchTemplate.deleteBatchData(esBasicInfo);
        spu1 = null;
        spu2 = null;
        spu3 = null;

        queryCondition = null;
        queryPair = null;
    }

    @Test
    public void constantScoreQuery() throws IOException {
        queryPair.setFieldNames(new String[] {"productName"});
        queryPair.setContent("android");

        QueryBuilder qb = QueryBuilders.termQuery(queryPair.getFieldNames()[0], queryPair.getContent());

        queryCondition.setQueryBuilder(QueryBuilders.constantScoreQuery(qb).boost(2.0f));

        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));
        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu1));
    }

    @Test
    public void disMaxQuery() throws IOException {
        QueryBuilder qb1 = QueryBuilders.termQuery("productName", "android");
        QueryBuilder qb2 = QueryBuilders.termQuery("brandName", "李宁");

        DisMaxQueryBuilder queryBuilder = QueryBuilders.disMaxQuery();
        queryBuilder.add(qb1);
        queryBuilder.add(qb2);
        queryBuilder.boost(1.3f).tieBreaker(0.7f);
        queryCondition.setQueryBuilder(queryBuilder);

        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));

        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu1));
    }

    @Test
    public void fuzzyQuery() throws IOException {
        queryPair.setFieldNames(new String[] {"brandName"});
        queryPair.setContent("李");
        queryCondition.setQueryBuilder(QueryBuilders.fuzzyQuery(queryPair.getFieldNames()[0], queryPair.getContent()));
        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));
        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());
        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu2));

        queryPair.setContent("李宁");
        queryCondition.setQueryBuilder(QueryBuilders.fuzzyQuery(queryPair.getFieldNames()[0], queryPair.getContent()));
        list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));
        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());
        assertThat(list.size(), is(0));
    }

    @Test
    public void highLightResultSet() {
        HighLight highLight = new HighLight();
        HighlightBuilder hBuilder = new HighlightBuilder();
        hBuilder.preTags("<h2>");
        hBuilder.postTags("</h2>");
        hBuilder.field("productName");

        queryPair.setFieldNames(new String[] {"productName"});
        queryPair.setContent("*");
        queryCondition.setSearchType(SearchType.QUERY_THEN_FETCH);
        queryCondition
            .setQueryBuilder(QueryBuilders.wildcardQuery(queryPair.getFieldNames()[0], queryPair.getContent()));

        highLight.setBuilder(hBuilder);
        highLight.setField("productName");

        List<Map<String, Object>> sourceList =
            elasticsearchTemplate.highLightResultSet(esBasicInfo, queryCondition, highLight);

        assertThat(sourceList.size(), is(3));
        assertThat((String)sourceList.get(0).get("productName"), containsString("运"));
        assertThat((String)sourceList.get(1).get("productName"), containsString("android"));
        assertThat((String)sourceList.get(2).get("productName"), containsString("华"));
    }

    @Test
    public void idsQuery() throws IOException {
        String[] idsArray = new String[] {"1"};
        queryCondition.setQueryBuilder(QueryBuilders.idsQuery().addIds(idsArray));
        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));
        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu1));

        idsArray = new String[] {"1", "2", "3"};
        queryCondition.setQueryBuilder(QueryBuilders.idsQuery().addIds(idsArray));
        list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));
        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(3));
        assertThat(list.get(0), equalTo(spu2));
        assertThat(list.get(1), equalTo(spu1));
        assertThat(list.get(2), equalTo(spu3));
    }

    @Before
    public void init() throws InterruptedException, IOException {
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

        spu3 = new Spu();
        spu3.setProductCode("XYY1234567");
        spu3.setProductName("中华人民共和国");
        spu3.setBrandCode("YD-001");
        spu3.setBrandName("米老鼠");
        spu3.setCategoryCode("YDC-001");
        spu3.setCategoryName("服装");

        Sku sku31 = new Sku("LS001", "老鼠的帽子1", "Red", "L", 4000);
        Sku sku32 = new Sku("LS002", "老鼠的帽子2", "Yellow", "M", 3000);
        Sku sku33 = new Sku("LS003", "老鼠的帽子3", "Green", "2XL", 5000);

        spu3.getSkus().add(sku31);
        spu3.getSkus().add(sku32);
        spu3.getSkus().add(sku33);

        esBasicInfo = new ESBasicInfo();
        esBasicInfo.setIndex("es_test");
        esBasicInfo.setType("type");

        esBasicInfo.setIds(new String[] {"1"});
        elasticsearchTemplate.addData(esBasicInfo, spu1);

        esBasicInfo.setIds(new String[] {"2"});
        elasticsearchTemplate.addData(esBasicInfo, spu2);

        esBasicInfo.setIds(new String[] {"3"});
        elasticsearchTemplate.addData(esBasicInfo, spu3);
        Thread.sleep(1500);

        queryCondition = new QueryCondition();
        queryPair = new QueryPair();
    }

    @Test
    public void matchAllQuery() throws IOException {
        queryCondition.setQueryBuilder(QueryBuilders.matchAllQuery());
        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));

        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(3));
        assertThat(list.get(0), equalTo(spu2));
        assertThat(list.get(1), equalTo(spu1));
        assertThat(list.get(2), equalTo(spu3));
    }

    @Test
    public void matchPhrasePrefixQuery() throws IOException {
        queryPair.setFieldNames(new String[] {"productName"});
        queryPair.setContent("人民共");
        queryCondition.setQueryBuilder(
            QueryBuilders.matchPhrasePrefixQuery(queryPair.getFieldNames()[0], queryPair.getContent()));

        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));
        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());
        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu3));

        queryPair.setFieldNames(new String[] {"brandName"});
        queryPair.setContent("鼠");
        queryCondition.setQueryBuilder(
            QueryBuilders.matchPhrasePrefixQuery(queryPair.getFieldNames()[0], queryPair.getContent()));
        list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));
        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());
        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu3));
    }

    @Test
    public void matchQuery() throws IOException {
        queryPair.setFieldNames(new String[] {"productName"});
        queryPair.setContent("android手机");
        queryCondition.setQueryBuilder(QueryBuilders.matchQuery(queryPair.getFieldNames()[0], queryPair.getContent()));

        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));

        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());
        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu1));

        queryPair.setContent("android");
        queryCondition.setQueryBuilder(QueryBuilders.matchQuery(queryPair.getFieldNames()[0], queryPair.getContent()));
        list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));

        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());
        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu1));

        queryPair.setContent("xxx");
        queryCondition.setQueryBuilder(QueryBuilders.matchQuery(queryPair.getFieldNames()[0], queryPair.getContent()));
        list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));

        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());
        assertThat(list.size(), is(0));
    }

    @Test
    public void multiMatchQuery() throws IOException {
        queryPair.setFieldNames(new String[] {"productName"});
        queryPair.setContent("人民共和");
        queryCondition
            .setQueryBuilder(QueryBuilders.multiMatchQuery(queryPair.getContent(), queryPair.getFieldNames()));

        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));

        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu3));
    }

    @Test
    public void query() throws IOException {
        esBasicInfo.setIds(new String[] {"id"});
        Spu spu = elasticsearchTemplate.query(esBasicInfo, Spu.class);
        assertThat(spu, equalTo(null));

        esBasicInfo.setIds(new String[] {"3"});
        spu = elasticsearchTemplate.query(esBasicInfo, Spu.class);
        log.info("json string is:{}", mapper.writeValueAsString(spu));

        assertThat(spu, equalTo(spu3));
    }

    @Test
    public void queryStringQuery() throws IOException {
        queryCondition.setQueryBuilder(QueryBuilders.queryStringQuery("+android"));
        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));

        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu1));
    }

    @Test
    public void rangeQuery() throws IOException {
        queryPair.setFieldNames(new String[] {"productName"});
        QueryBuilder queryBuilder = QueryBuilders.rangeQuery(queryPair.getFieldNames()[0]).from("android").to("服装")
            .includeLower(true).includeUpper(true);
        queryCondition.setQueryBuilder(queryBuilder);

        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));
        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(3));
        assertThat(list.get(0), equalTo(spu2));
        assertThat(list.get(1), equalTo(spu1));
        assertThat(list.get(2), equalTo(spu3));
    }

    @Test
    public void scrollQuery() throws IOException {
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.setQueryBuilder(QueryBuilders.matchAllQuery());
        queryCondition.setSize(10);

        SearchResponse scrollResponse = elasticsearchTemplate.executeQuery("es_test", queryCondition, "type");

        String scrollId = scrollResponse.getScrollId();
        long total = scrollResponse.getHits().totalHits;
        log.info("\nscrollId is:{}\n", scrollId);
        log.info("\ntotal is:{}\n", total);

        List<Spu> content;
        if (total < 10) {
            content = elasticsearchTemplate.analyzeSearchResponse(Spu.class, scrollResponse);
            assertThat(content.size(), is(3));
        } else {
            for (int i = 0, sum = 0; sum < total; i++) {
                content = elasticsearchTemplate.analyzeSearchResponse(Spu.class, esClient.prepareSearchScroll(scrollId)
                    .setScroll(TimeValue.timeValueMinutes(8)).execute().actionGet());

                sum += 10;
                log.info("\n总量{} 已经查到{}", total, sum);
                assertThat(content.size(), is(10));
            }
        }
    }

    @Test
    public void spanQuery() throws IOException {
        queryPair.setFieldNames(new String[] {"productName"});
        queryPair.setContent("android");
        SpanQueryBuilder queryBuilder = QueryBuilders
            .spanFirstQuery(QueryBuilders.spanTermQuery(queryPair.getFieldNames()[0], queryPair.getContent()), 30000);
        queryCondition.setQueryBuilder(queryBuilder);

        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));

        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu1));

        queryBuilder = QueryBuilders.spanTermQuery(queryPair.getFieldNames()[0], queryPair.getContent());
        queryCondition.setQueryBuilder(queryBuilder);

        list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));

        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu1));
    }

    @Test
    public void termQuery() throws IOException {
        queryPair.setFieldNames(new String[] {"productName"});
        queryPair.setContent("android");
        queryCondition.setQueryBuilder(QueryBuilders.termQuery(queryPair.getFieldNames()[0], queryPair.getContent()));
        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));

        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu1));

        queryPair.setContent("android手机");
        queryCondition.setQueryBuilder(QueryBuilders.termQuery(queryPair.getFieldNames()[0], queryPair.getContent()));
        list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));

        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(0));
    }

    @Test
    public void wildcardQuery() throws IOException {

        queryPair.setFieldNames(new String[] {"productName"});
        queryPair.setContent("*");
        queryCondition
            .setQueryBuilder(QueryBuilders.wildcardQuery(queryPair.getFieldNames()[0], queryPair.getContent()));
        List<Spu> list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));
        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(3));
        assertThat(list.get(0), equalTo(spu2));
        assertThat(list.get(1), equalTo(spu1));
        assertThat(list.get(2), equalTo(spu3));

        queryPair.setFieldNames(new String[] {"productName"});
        queryPair.setContent("an*d");
        queryCondition
            .setQueryBuilder(QueryBuilders.wildcardQuery(queryPair.getFieldNames()[0], queryPair.getContent()));
        list = elasticsearchTemplate.analyzeSearchResponse(Spu.class,
            elasticsearchTemplate.executeQuery("es_test", queryCondition, "type"));
        log.info("json string is:{}", mapper.writeValueAsString(list));
        log.info("list size is:{}", list.size());

        assertThat(list.size(), is(1));
        assertThat(list.get(0), equalTo(spu1));
    }
}
