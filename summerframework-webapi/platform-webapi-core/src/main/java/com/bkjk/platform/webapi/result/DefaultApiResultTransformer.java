package com.bkjk.platform.webapi.result;

import com.bkjk.platform.webapi.exception.ApiException;
import com.bkjk.platform.webapi.exception.ApiExceptionWithData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.servlet.http.HttpServletRequest;
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
        }
        if (ex instanceof ApiException) {
            return handleApiException(request, (ApiException) ex);
        } else {
            return handleException(request, ex);
        }
    }

    @Override
    public Class<? extends ApiResultWrapper> getType() {
        return ApiResult.class;
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
