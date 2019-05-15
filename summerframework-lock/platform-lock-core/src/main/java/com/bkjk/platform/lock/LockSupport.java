package com.bkjk.platform.lock;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 15:23
 **/
public class LockSupport {

    public static LockInstance getCurrentLock(){
        return LockAspect.lockInfoThreadLocal.get();
    }
}
