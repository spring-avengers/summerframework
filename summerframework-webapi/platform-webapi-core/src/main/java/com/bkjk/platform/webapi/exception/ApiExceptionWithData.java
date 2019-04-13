package com.bkjk.platform.webapi.exception;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/3/29 18:42
 **/
public class ApiExceptionWithData extends ApiException {

    private Object data;

    public ApiExceptionWithData(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public ApiExceptionWithData(Throwable cause, String errorCode, String errorMessage) {
        super(cause, errorCode, errorMessage);
    }

    public ApiExceptionWithData(String errorCode, String errorMessage,Object data) {
        this(null,errorCode,errorMessage,data);
    }

    public ApiExceptionWithData(Throwable cause, String errorCode, String errorMessage,Object data) {
        super(cause, errorCode, errorMessage);
        this.data=data;
    }

    public Object getData() {
        return data;
    }
}
