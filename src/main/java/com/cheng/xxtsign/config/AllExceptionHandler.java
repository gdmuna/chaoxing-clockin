package com.cheng.xxtsign.config;

import com.cheng.xxtsign.common.CommonResult;
import com.cheng.xxtsign.enums.ResultCode;
import com.cheng.xxtsign.exception.user.XXTUserException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class AllExceptionHandler {

    /**
     * 用户异常问题
     * @param ex
     * @return
     */
    @ExceptionHandler(XXTUserException.class)
    public CommonResult doUserNameException(XXTUserException ex){
        return CommonResult.failed(ResultCode.USERNAMEEXCEPTION, ex.getMessage());
    }
}