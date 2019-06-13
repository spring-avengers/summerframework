package com.bkjk.platform.elasticsearch.wrapper;

import lombok.Data;

@Data
public class ESBasicInfo {

    private String index;

    private String type;

    private String[] ids;
}
