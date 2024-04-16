package com.cheng.xxtsign.mapper.local;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cheng.xxtsign.exception.user.XXTUserException;
import com.cheng.xxtsign.mapper.XXTUserMapper;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class XXTUserMapperImpl implements XXTUserMapper {

    @Override
    public JSONObject getUserByPhone(String phone) {
        // 获取原先数据
        String filePath = "user.json"; // 指定的文件目录

        JSONArray jsonArray = null;

        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            if (StringUtils.isEmpty(content)) {
                jsonArray = new JSONArray();
            }else {
                jsonArray = JSONArray.parseArray(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new XXTUserException("用户数据io错误，请联系开发者");
        }

        if (!ObjectUtil.isEmpty(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                if (obj.getString("phone").equals(phone)) {
                    return obj;
                }
            }
        }

        return null;
    }

    @Override
    public JSONArray getUserListByMark(String mark) {
        return null;
    }
}
