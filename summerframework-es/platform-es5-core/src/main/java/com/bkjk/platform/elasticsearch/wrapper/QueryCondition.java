package com.bkjk.platform.elasticsearch.wrapper;

import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;

import lombok.Data;

@Data
public class QueryCondition {

    private long millis = 1000;

    private int size = 50;

    private QueryBuilder queryBuilder;

    private QueryPair queryPair;

    private SearchType searchType = SearchType.DFS_QUERY_THEN_FETCH;
}
