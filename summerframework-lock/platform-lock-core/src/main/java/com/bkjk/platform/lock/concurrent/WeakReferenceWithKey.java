package com.bkjk.platform.lock.concurrent;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/16 15:00
 **/
public class WeakReferenceWithKey<T> extends WeakReference<T> {
    private String key;

    public WeakReferenceWithKey(T referent, String key) {
        super(referent);
        this.key = key;
    }

    public WeakReferenceWithKey(T referent, ReferenceQueue q, String key) {
        super(referent, q);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
