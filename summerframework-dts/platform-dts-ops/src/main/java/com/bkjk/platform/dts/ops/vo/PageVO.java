package com.bkjk.platform.dts.ops.vo;

import java.io.Serializable;
import java.util.List;

public class PageVO<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long total;
    private final List<T> rows;
    private final int page;
    private final int size;

    public PageVO(List<T> rows, long total, int page, int size) {
        this.total = total;
        this.rows = rows;
        this.page = page;
        this.size = size;
    }

    public int getPage() {
        return page;
    }

    public List<T> getRows() {
        return rows;
    }

    public int getSize() {
        return size;
    }

    public long getTotal() {
        return total;
    }

}
