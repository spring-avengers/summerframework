package com.bkjk.platform.lock;

import java.util.Map;
import java.util.Set;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 15:29
 **/
public interface LockMonitor {
    /**
     * 返回lock的名字，是否已经锁定
     * @return
     */
    Set<String> getLockNames();

    /**
     * 获取锁的持有者
     * @return
     */
    Map<String,Thread> getLockOwners();
}
