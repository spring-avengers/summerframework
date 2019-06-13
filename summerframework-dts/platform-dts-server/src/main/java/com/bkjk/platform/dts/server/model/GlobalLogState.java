package com.bkjk.platform.dts.server.model;

public enum GlobalLogState {

    Begin(1),

    Committed(2),

    Rollbacked(3),

    CmmittedFailed(4),

    RollbackFailed(5),

    Commiting(6),

    Rollbacking(7);

    public static GlobalLogState parse(int value) {
        for (GlobalLogState state : GlobalLogState.values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return null;
    }

    private int value;

    private GlobalLogState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
