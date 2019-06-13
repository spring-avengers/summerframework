package com.bkjk.platform.dts.ops.vo;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum BranchLogState {

    Begin(1, "分支开始"),

    Success(2, "分支成功"),

    Failed(3, "分支失败");

    public static String getNameByStateValue(int value) {
        return Arrays.stream(BranchLogState.values()).filter((v) -> {
            return v.value == value;
        }).collect(Collectors.toList()).get(0).getName();
    }

    private int value;

    private String name;

    private BranchLogState(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }
}
