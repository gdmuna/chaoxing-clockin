package com.cheng.xxtsign.service;

public interface XXTUserService {

    /**
     * 用户登录
     * @param phone
     * @param password
     * @return
     */
    boolean userLogin(String phone, String password);


}
