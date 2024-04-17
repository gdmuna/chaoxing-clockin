package com.cheng.xxtsign.service;

import com.cheng.xxtsign.dao.vo.XXTUserVO;

import java.util.List;

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
    List<XXTUserVO> getUserListByMark(String mark);

    /**
     * 创建一个新组
     * @param mark 组标识
     * @param au 权限标识
     */
    void addGroup(String mark, String au);

    /**
     * 从组内移除成员
     * @param mark
     * @param phone
     * @param au
     */
    boolean delUser(String mark, String phone, String au);
}
