package com.bkjk.platform.elasticsearch.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;

import com.bkjk.platform.elasticsearch.wrapper.ESBasicInfo;
import com.bkjk.platform.elasticsearch.wrapper.HighLight;
import com.bkjk.platform.elasticsearch.wrapper.QueryCondition;
import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElasticsearchTemplate {
    @Resource
    private TransportClient esClient;

    private ObjectMapper mapper = new ObjectMapper();

    public int addBatchData(ESBasicInfo esBasicInfo, Object object) throws IOException {
        BulkRequestBuilder bulkRequest = esClient.prepareBulk();

        for (String id : esBasicInfo.getIds()) {
            bulkRequest.add(esClient.prepareIndex(esBasicInfo.getIndex(), esBasicInfo.getType(), id)
                .setSource(mapper.writeValueAsString(object), XContentType.JSON));
        }
        bulkRequest.execute().actionGet();

        return bulkRequest.numberOfActions();
    }

    public boolean addData(ESBasicInfo esBasicInfo, Object object) throws IOException {
        IndexResponse result =
            esClient.prepareIndex(esBasicInfo.getIndex(), esBasicInfo.getType(), esBasicInfo.getIds()[0])
                .setSource(mapper.writeValueAsString(object), XContentType.JSON).get();

        return result.status().getStatus() == 201 || result.status().getStatus() == 200;
    }

    public <T> List<T> analyzeSearchResponse(Class<T> clazz, SearchResponse response) throws IOException {
        SearchHits searchHits = response.getHits();

        List<T> result = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            result.add(mapper.readValue(hit.getSourceAsString(), clazz));
        }
        return result;
    }

    public boolean createIndex(String index) {
        if (isExistedIndex(index)) {
            return false;
        }
        CreateIndexResponse indexResponse = esClient.admin().indices().prepareCreate(index).get();

        return indexResponse.isAcknowledged();
    }

    public boolean createMapping(String index, String type, XContentBuilder mapping) throws IOException {
        log.info("mapping is:{}", mapping.toString());

        PutMappingRequest mappingRequest = Requests.putMappingRequest(index).source(mapping).type(type);
        AcknowledgedResponse putMappingResponse = esClient.admin().indices().putMapping(mappingRequest).actionGet();
        return putMappingResponse.isAcknowledged();
    }

    public int deleteBatchData(ESBasicInfo esBasicInfo) {
        BulkRequestBuilder bulkRequest = esClient.prepareBulk();

        for (String id : esBasicInfo.getIds()) {
            bulkRequest.add(esClient.prepareDelete(esBasicInfo.getIndex(), esBasicInfo.getType(), id));
        }

        BulkResponse response = bulkRequest.execute().actionGet();
        log.info("status is:{}", response.status().getStatus());

        return bulkRequest.numberOfActions();
    }

    public boolean deleteData(ESBasicInfo esBasicInfo) {
        DeleteResponse result =
            esClient.prepareDelete(esBasicInfo.getIndex(), esBasicInfo.getType(), esBasicInfo.getIds()[0]).get();

        return result.status().getStatus() == 200;
    }

    public boolean deleteIndex(String index) {
        if (!isExistedIndex(index)) {
            return false;
        }
        AcknowledgedResponse deleteIndexResponse =
            esClient.admin().indices().prepareDelete(index).execute().actionGet();
        return deleteIndexResponse.isAcknowledged();
    }

    public SearchResponse executeQuery(String index, QueryCondition queryCondition, String... type) {
        return esClient.prepareSearch(index).setTypes(type).setSearchType(queryCondition.getSearchType())
            .setScroll(new TimeValue(queryCondition.getMillis())).setQuery(queryCondition.getQueryBuilder())
            .addSort(SortBuilders.fieldSort("_doc")).setSize(queryCondition.getSize()).execute().actionGet();
    }

    public List<Map<String, Object>> getAllMapping(String index) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        ImmutableOpenMap<String, MappingMetaData> mappings = esClient.admin().cluster().prepareState().execute()
            .actionGet().getState().getMetaData().getIndices().get(index).getMappings();

        for (ObjectObjectCursor<String, MappingMetaData> cursor : mappings) {
            log.info("type is:{}", cursor.key);
            result.add(cursor.value.getSourceAsMap());
        }
        return result;
    }

    public String getMapping(String index, String type) throws IOException {
        ImmutableOpenMap<String, MappingMetaData> mappings = esClient.admin().cluster().prepareState().execute()
            .actionGet().getState().getMetaData().getIndices().get(index).getMappings();

        return mappings.get(type).source().string();
    }

    public List<Map<String, Object>> highLightResultSet(ESBasicInfo esBasicInfo, QueryCondition queryCondition,
        HighLight highLight) {
        SearchResponse response = esClient.prepareSearch(esBasicInfo.getIndex()).setTypes(esBasicInfo.getType())
            .setSearchType(queryCondition.getSearchType()).setScroll(new TimeValue(queryCondition.getMillis()))
            .setQuery(queryCondition.getQueryBuilder()).setSize(queryCondition.getSize())
            .highlighter(highLight.getBuilder()).execute().actionGet();

        String highlightField = highLight.getField();
        List<Map<String, Object>> sourceList = new ArrayList<>();

        for (SearchHit searchHit : response.getHits()) {
            Map<String, Object> element = new HashMap<>();
            StringBuilder stringBuilder = new StringBuilder();
            if (StringUtils.isNotEmpty(highlightField)) {
                Text[] text = searchHit.getHighlightFields().get(highlightField).getFragments();

                if (text != null) {
                    for (Text str : text) {
                        stringBuilder.append(str.string());
                    }

                    log.info("遍历 高亮结果集{}", stringBuilder.toString());
                    element.put(highlightField, stringBuilder.toString());
                }
            }
            sourceList.add(element);
        }

        return sourceList;
    }

    public boolean isExistedIndex(String index) {
        return esClient.admin().indices().prepareExists(index).execute().actionGet().isExists();
    }

    public <T> T query(ESBasicInfo esBasicInfo, Class<T> clazz) throws IOException {
        GetRequestBuilder requestBuilder =
            esClient.prepareGet(esBasicInfo.getIndex(), esBasicInfo.getType(), esBasicInfo.getIds()[0]);
        GetResponse response = requestBuilder.execute().actionGet();

        return response.getSourceAsString() != null ? mapper.readValue(response.getSourceAsString(), clazz) : null;
    }

    public int updateBatchData(ESBasicInfo esBasicInfo, Object object) throws IOException {
        BulkRequestBuilder bulkRequest = esClient.prepareBulk();

        for (String id : esBasicInfo.getIds()) {
            bulkRequest.add(esClient.prepareUpdate(esBasicInfo.getIndex(), esBasicInfo.getType(), id)
                .setDoc(mapper.writeValueAsString(object), XContentType.JSON));
        }

        bulkRequest.execute().actionGet();

        return bulkRequest.numberOfActions();
    }

    public boolean updateData(ESBasicInfo esBasicInfo, Object object) throws IOException {
        UpdateResponse result =
            esClient.prepareUpdate(esBasicInfo.getIndex(), esBasicInfo.getType(), esBasicInfo.getIds()[0])
                .setDoc(mapper.writeValueAsString(object), XContentType.JSON).get();

        return result.status().getStatus() == 200;
    }
}
