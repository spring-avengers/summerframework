package com.bkjk.platform.dts.common;

import java.sql.SQLException;

public class DtsLockConflictException extends SQLException {

    private static final long serialVersionUID = 1L;

    public DtsLockConflictException() {
    }

    public DtsLockConflictException(String reason) {
        super(reason);
    }
}
