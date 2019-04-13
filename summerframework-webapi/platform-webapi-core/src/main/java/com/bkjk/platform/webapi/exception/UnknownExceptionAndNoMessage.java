package com.bkjk.platform.webapi.exception;

/**
 * @Program: summerframework2
 * @Description: 应用的异常必须包含非空且人类可读的 message，如果应用抛出了没有message的异常(如NullPointerException)，则转化为该异常。
 * @Author: shaoze.wang
 * @Create: 2019/3/8 13:43
 **/
public class UnknownExceptionAndNoMessage extends ApiException{
    public UnknownExceptionAndNoMessage(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public UnknownExceptionAndNoMessage(Throwable cause, String errorCode, String errorMessage) {
        super(cause, errorCode, errorMessage);
    }
}
