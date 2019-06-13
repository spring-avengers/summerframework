package com.bkjk.platform.elasticsearch.wrapper;

import lombok.Data;

@Data
public class QueryPair {

    private String[] fieldNames;

    private String content;
}
