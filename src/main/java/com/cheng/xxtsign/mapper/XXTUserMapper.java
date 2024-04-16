package com.cheng.xxtsign.mapper;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

public interface XXTUserMapper {

    JSONObject getUserByPhone(String phone);

    JSONArray getUserListByMark(String mark);

    int insertUserToGroup(JSONObject user, String mark);
}
