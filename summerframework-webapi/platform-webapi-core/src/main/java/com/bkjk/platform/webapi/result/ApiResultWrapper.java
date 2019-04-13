package com.bkjk.platform.webapi.result;

import com.bkjk.platform.webapi.version.Constant;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;


/**
 * @Program: summerframework2
 * @Description: 业务中的ResponseVo实现该接口的话可以被框架识别。
 * @Author: shaoze.wang
 * @Create: 2019/3/7 10:12
 **/
public interface ApiResultWrapper<T> {

    /**
     * 只有成功的请求才能返回True
     * @return
     */
    @NonNull
    boolean isSuccess();

    /**
     * 必须有code
     * @return
     */
    @NonNull
    String getCode();

    /**
     * 只有出错时才能有error，error是系统错误得简单描述，一般用英文字母，方便开发人员查错使用，成功的请求error必须为null
     * @return
     */
    @Nullable
    String getError();

    /**
     * message 必须是人能理解得描述信息，会展示给前端查看
     * @return
     */
    @NonNull
    String getMessage();

    /**
     * 请求的url地址
     * @return
     */
    @Nullable
    String getPath();

    /**
     * 返回值的创建时间
     * @return
     */
    @NonNull
    default long getTime(){
        return System.currentTimeMillis();
    }

    /**
     * 返回的业务数据
     * @return
     */
    @Nullable
    T getData();

    /**
     * 返回schema的版本，框架返回"S2"的字样，其它业务如果自定义了自己的返回值字段，那么这里必须使用不同的标识。这个字段是为了向后兼容添加的。
     * @return
     */
    @NonNull
    default String getSchemaVersion() {
        return Constant.VERSION_EMPTY;
    }

}
