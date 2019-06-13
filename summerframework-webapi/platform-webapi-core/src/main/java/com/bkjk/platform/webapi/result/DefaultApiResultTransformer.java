package com.bkjk.platform.webapi.result;

import com.bkjk.platform.webapi.exception.ApiException;
import com.bkjk.platform.webapi.exception.ApiExceptionWithData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.stream.Collectors;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/3/7 10:28
 **/
@Slf4j
public class DefaultApiResultTransformer implements ApiResultTransformer {

    @Override
    public ApiResultWrapper changeBody(Object body, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        return ApiResult.builder().time(System.currentTimeMillis()).success(true)
                .code(String.valueOf(HttpStatus.OK.value())).data(body).error("").message(HttpStatus.OK.getReasonPhrase())
                .path(request.getURI().getPath()).build();
    }

    @Override
    public ApiResultWrapper<Object> exceptionToResult(HttpServletRequest request, Exception ex) {
        if (ex instanceof MethodArgumentNotValidException) {
            return handleMethodArgumentNotValidException(request, (MethodArgumentNotValidException) ex);
        }else if (ex instanceof ApiException) {
            return handleApiException(request, (ApiException) ex);
        }
        Object errorCode = getStringValueFromField(ex, ERROR_CODE_FIELD);
        Object errorMessage = getStringValueFromField(ex, ERROR_MESSAGE_FIELD);
        if (errorCode!=null) {
            return handleExceptionWithCode(request,ex,errorCode,errorMessage);
        } else {
            return handleException(request, ex);
        }
    }

    @Override
    public Class<? extends ApiResultWrapper> getType() {
        return ApiResult.class;
    }

    private Object getStringValueFromField(Object o, String fieldName){
        Field field = ReflectionUtils.findField(o.getClass(), fieldName);
        if(field==null){
            return null;
        }
        field.setAccessible(true);
        try {
            return field.get(o);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private final static String ERROR_CODE_FIELD="errorCode";
    private final static String ERROR_MESSAGE_FIELD="errorMessage";
    private final static String DATA_FIELD="data";

    public ApiResult handleExceptionWithCode(HttpServletRequest request, Exception ex,Object errorCode,Object errorMessage) {
        Object data = getStringValueFromField(ex, DATA_FIELD);
        return ApiResult.builder()
                .time(System.currentTimeMillis())
                .success(false)
                .code(errorCode.toString())
                .data(data)
                .error(ex.getClass().getName())
                .message(errorMessage==null?ex.getMessage():errorMessage.toString())
                .path(request.getRequestURI())
                .build();
    }

    public ApiResult handleApiException(HttpServletRequest request, ApiException ex) {
        return ApiResult.builder()
                .time(System.currentTimeMillis())
                .success(false)
                .code(ex.getErrorCode())
                .data(ex instanceof ApiExceptionWithData ? ((ApiExceptionWithData) ex).getData() : null)
                .error(ex.getClass().getName())
                .message(ex.getErrorMessage())
                .path(request.getRequestURI())
                .build();
    }

    public ApiResult handleException(HttpServletRequest request, Exception ex) {
        return ApiResult.builder().time(System.currentTimeMillis()).success(false)
                .code(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())).data(null)
                .error(ex.getClass().getName()).message(ex.getMessage())
                .path(request.getRequestURI()).build();
    }

    public ApiResult handleMethodArgumentNotValidException(HttpServletRequest request,
                                                           MethodArgumentNotValidException ex) {
        String message = "Validating request parameter failed ("
                + ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> String.format("Field：%s Value：%s Reason：%s", fieldError.getField(),
                        fieldError.getRejectedValue(), fieldError.getDefaultMessage()))
                .collect(Collectors.joining("; "))
                + ")";
        return ApiResult.builder().time(System.currentTimeMillis()).success(false)
                .code(String.valueOf(HttpStatus.UNPROCESSABLE_ENTITY.value())).data(null)
                .error(ex.getClass().getName()).message(message).path(request.getRequestURI())
                .build();
    }
}
