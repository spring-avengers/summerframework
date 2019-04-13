package com.bkjk.platform.mybatis.injector;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.bkjk.platform.mybatis.injector.methods.InsertBatch;
import com.bkjk.platform.mybatis.injector.methods.SaveOrUpdate;
import com.bkjk.platform.mybatis.injector.methods.UpdateBatchById;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class MySqlInjector extends DefaultSqlInjector {

    @Override
    public List<AbstractMethod> getMethodList() {
        List<AbstractMethod> methodArrayList = new ArrayList<>();
        methodArrayList.addAll(super.getMethodList());
        methodArrayList.addAll(Stream.of(//
            new InsertBatch(), //
            new SaveOrUpdate(), //
            new UpdateBatchById()//
        ).collect(toList()));
        return methodArrayList;
    }

}
