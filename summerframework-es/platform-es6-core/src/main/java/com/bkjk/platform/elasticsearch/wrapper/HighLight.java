package com.bkjk.platform.elasticsearch.wrapper;

import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

import lombok.Data;

@Data
public class HighLight {

    private HighlightBuilder builder;

    private String field;
}
