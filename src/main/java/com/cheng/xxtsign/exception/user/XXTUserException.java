package com.cheng.xxtsign.exception.user;

import com.cheng.xxtsign.exception.BaseException;

public class XXTUserException extends BaseException {
    public XXTUserException(){
        super();
    }

    public XXTUserException(String message, Throwable cause) {
        super(message, cause);
    }

    public XXTUserException(String message) {
        super(message);
    }

    public XXTUserException(Throwable cause) {
        super(cause);
    }
}
