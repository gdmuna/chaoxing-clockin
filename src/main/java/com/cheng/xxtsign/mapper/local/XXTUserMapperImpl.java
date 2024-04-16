package com.cheng.xxtsign.mapper.local;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cheng.xxtsign.exception.user.XXTUserException;
import com.cheng.xxtsign.mapper.XXTUserMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
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
        String fileName = mark + ".json";
        File file = new File(fileName);

        JSONArray jsonArray = null;
        if (file.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(fileName)));
                if (StringUtils.isEmpty(content)) {
                    jsonArray = new JSONArray();
                }else {
                    jsonArray = JSONArray.parseArray(content);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return jsonArray;
    }

    @Override
    public int insertUserToGroup(JSONObject user, String mark) {
        String fileName = mark + ".json";
        File file = new File(fileName);
        String phone = user.getString("phone");

        JSONArray jsonArray = null;
        if (file.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(fileName)));
                if (StringUtils.isEmpty(content)) {
                    jsonArray = new JSONArray();
                }else {
                    jsonArray = JSONArray.parseArray(content);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            jsonArray = new JSONArray();
        }

        // 构建对象
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("phone", phone);
        jsonObject.put("U_SName", user.getString("U_SName"));
        jsonObject.put("Login_Sign_System_Time", user.getString("Login_Sign_System_Time"));

        // 检查在不在
        boolean found = false;

        if (!ObjectUtil.isEmpty(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                if (obj.getString("phone").equals(phone)) {
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            jsonArray.add(jsonObject);
        }
        // 保存
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(jsonArray.toJSONString());
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
