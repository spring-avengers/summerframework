package com.bkjk.platfrom.dts.core.resource.mysql.common;

import java.io.Serializable;

public class PkPair<K, V> implements Serializable {

    private static final long serialVersionUID = 2659298587801683491L;
    public K key;
    public V value;

    public PkPair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public PkPair() {
    }

    public static <K, V> PkPair<K, V> of(K key, V value) {
        return new PkPair(key, value);
    }

    public final K getKey() {
        return this.key;
    }

    public V getValue() {
        return this.value;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public String toString() {
        return "(" + this.getKey() + ',' + this.getValue() + ')';
    }

}
