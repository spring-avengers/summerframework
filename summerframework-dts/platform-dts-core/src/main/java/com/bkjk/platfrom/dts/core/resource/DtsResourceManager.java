package com.bkjk.platfrom.dts.core.resource;

import com.bkjk.platform.dts.common.DtsException;

public interface DtsResourceManager {

    public void branchCommit(long xid, long branchId) throws DtsException;

    public void branchRollback(long xid, long branchId) throws DtsException;

    public long register() throws DtsException;

}
