package com.cheng.xxtsign.utils;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class HeadersUtils {

    public static Map<String, String> headers = new HashMap<>();

    static {
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");
        headers.put("Cache-Control", "no-cache");
        headers.put("Connection", "keep-alive");
        headers.put("Pragma", "no-cache");
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "none");
        headers.put("X-Requested-With", "com.chaoxing.mobile");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("User-Agent", "YOUR_USER_AGENT_STRING");
        headers.put("sec-ch-ua", "\"Not A(Brand\";v=\"99\", \"Android WebView\";v=\"121\", \"Chromium\";v=\"121\"");
        headers.put("sec-ch-ua-mobile", "?1");
        headers.put("sec-ch-ua-platform", "\"Android\"");
    }

    public static Map<String, String> getHeaders() {
        return headers;
    }

    public static String encrypt(String msg, String key, String iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes("UTF-8"));
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] encrypted = cipher.doFinal(msg.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void storeUser(String phoneNumber, JSONObject newObj) {
        // 获取原先数据
        String filePath = "user.json"; // 指定的文件目录

        File file = new File(filePath);
        JSONArray jsonArray;

        newObj.put("phone", phoneNumber);

        // 判断文件是否存在
        if (file.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(filePath)));
                if (StringUtils.isEmpty(content)) {
                    jsonArray = new JSONArray();
                }else {
                    jsonArray = JSONArray.parseArray(content);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            jsonArray = new JSONArray();
        }

        boolean found = false;

        if (!ObjectUtil.isEmpty(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                if (obj.getString("phone").equals(phoneNumber)) {
                    // 更新对象的属性
                    jsonArray.set(i, newObj);
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            // JSONObject newObj = new JSONObject();
//            newObj.put("phone", phoneNumber);
//            newObj.put("name", "newName");
            // 添加其他属性

            jsonArray.add(newObj);
        }

        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(jsonArray.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject getUser(String phoneNumber) {
        // 获取原先数据
        String filePath = "user.json"; // 指定的文件目录

        JSONArray jsonArray;
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            if (StringUtils.isEmpty(content)) {
                jsonArray = new JSONArray();
            }else {
                jsonArray = JSONArray.parseArray(content);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!ObjectUtil.isEmpty(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                if (obj.getString("phone").equals(phoneNumber)) {
                    obj.remove("phone");

                    return obj;
                }
            }
        }
        return null;
    }

    public static String jsonToHeader(JSONObject jsonObject) {

        String queryString = String.format("fid=%s; uf=%s; _d=%s; UID=%s; vc3=%s;",
                jsonObject.get("fid"), jsonObject.get("uf"), jsonObject.get("_d"), jsonObject.get("_uid") != null ? jsonObject.get("_uid") : jsonObject.get("UID"), jsonObject.get("vc3"));

        System.out.println(queryString);

        return queryString;
    }
}
