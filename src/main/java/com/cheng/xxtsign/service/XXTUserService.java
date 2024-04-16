package com.cheng.xxtsign.service;

public interface XXTUserService {

    /**
     * 用户登录
     * @param phone
     * @param password
     * @return
     */
    boolean userLogin(String phone, String password);


    boolean join(String mark, String phone);

    /**
     * 获取加入组的成员信息
     * @param mark 组标识
     */
    void getUserListByMark(String mark);
}
