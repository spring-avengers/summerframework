package com.bkjk.platform.dts.ops.vo;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum GlobalLogState {

    Begin(1, "开始"),

    Committed(2, "已提交"),

    Rollbacked(3, "已回滚"),

    CmmittedFailed(4, "提交失败"),

    RollbackFailed(5, "回滚失败"),

    Commiting(6, "提交中"),

    Rollbacking(7, "回滚中");

    public static String getNameByStateValue(int value) {
        return Arrays.stream(GlobalLogState.values()).filter((v) -> {
            return v.value == value;
        }).collect(Collectors.toList()).get(0).getName();
    }

    public static GlobalLogState parse(int value) {
        for (GlobalLogState state : GlobalLogState.values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return null;
    }

    private int value;

    private String name;

    private GlobalLogState(int value, String name) {
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
