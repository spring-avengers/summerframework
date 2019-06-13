package com.bkjk.platform.elasticsearch.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class Spu {

    private String productCode;

    private String productName;

    private String brandCode;

    private String brandName;

    private String categoryCode;

    private String categoryName;

    private String imageTag;

    private List<Sku> skus = new ArrayList<>();
}
