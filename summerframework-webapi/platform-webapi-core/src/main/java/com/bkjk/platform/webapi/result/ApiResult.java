package com.bkjk.platform.webapi.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import static com.bkjk.platform.webapi.version.Constant.VERSION_SUMMER2;

@Data
@Builder
public class ApiResult<T> implements ApiResultWrapper<T> {
    boolean success;
    String code;
    String error;
    String message;
    String path;
    long time;
    T data;

    @Override
    @JsonIgnore
    public String getSchemaVersion() {
        return VERSION_SUMMER2;
    }

}
