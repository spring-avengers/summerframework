package com.bkjk.platform.dts.server.model;

public enum BranchLogState {

    Begin(1),

    Success(2),

    Failed(3);

    private int value;

    private BranchLogState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
