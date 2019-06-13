package com.bkjk.platform.elasticsearch.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sku {

    private String skuCode;

    private String skuName;

    private String color;

    private String size;

    private int skuPrice;

}
