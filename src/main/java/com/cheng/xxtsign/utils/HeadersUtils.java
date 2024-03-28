package com.cheng.xxtsign.utils;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cheng.xxtsign.vo.CourseVo;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

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

    /**
     * 保存用户phone到组文件
     * @param mark
     * @param phone
     * @return
     */
    public static boolean storeUserJoinGroup(String mark, String phone) {
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
        }else {
            jsonArray = new JSONArray();
        }

        // 构建对象
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("phone", phone);

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

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    
    public static boolean hasUser(String phone) {
        if (getUser(phone) == null) {
            return false;
        }
        
        return true;
    }
    
    public static boolean hasJsonFile(String mark) {
        String fileName = mark + ".json";
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        }
        return false;
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

    /**
     * 将保存的用户cookie转化为String
     * @param jsonObject
     * @return
     */
    public static String jsonToHeader(JSONObject jsonObject) {

        String queryString = String.format("fid=%s; uf=%s; _d=%s; UID=%s; vc3=%s;",
                jsonObject.get("fid"), jsonObject.get("uf"), jsonObject.get("_d"), jsonObject.get("_uid") != null ? jsonObject.get("_uid") : jsonObject.get("UID"), jsonObject.get("vc3"));

        System.out.println(queryString);

        return queryString;
    }

    /**
     * POST
     * @param url
     * @param method
     * @param header
     * @param
     * @return
     */
    public static Response requestToXXT(String url, String method, Map<String, String> header, Map<String, String> param) {
        OkHttpClient client = new OkHttpClient();
        client = client.newBuilder().build();

        // 请求头
        Request.Builder builder = new Request.Builder();
        for (Map.Entry<String, String> entry : header.entrySet()){
            builder.addHeader(entry.getKey(), entry.getValue());
        }

        // 设置请求方式, 默认GET
        Request request;
        if (method.equals("GET")) {
            request = builder.url(url).get().build();
        } else if (method.equals("POST")) {
            // 请求体
            RequestBody requestBody = null;
            if (!MapUtil.isEmpty(param)) {
                FormBody.Builder builder1 = new FormBody.Builder();
                for (Map.Entry<String, String> entry : param.entrySet()){
                    builder1.add(entry.getKey(), entry.getValue());
                }
                requestBody = builder1.build();
            }
            request = builder.url(url).post(requestBody).build();
        } else {
            // 默认GET
            request = builder.url(url).get().build();
        }


        try {
            Response response = client.newCall(request).execute();
            return response;
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * GET 参数全放URL
     * @param url
     * @param method
     * @param header
     * @return
     */
    public static Response requestToXXT(String url, String method, Map<String, String> header) {
        return requestToXXT(url, method, header, null);
    }

    public static Map<String, String> objectToMap(Object obj) {
        Map<String, String> map = new HashMap<>();
        Field[] fields = obj.getClass().getDeclaredFields();

        for (Field field : fields) {
            try {
                field.setAccessible(true);
                if (!Modifier.isStatic(field.getModifiers())) {
                    Object value = field.get(obj);
                    if (value != null) {
                        map.put(field.getName(), value.toString());
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return map;
    }

    @NotNull
    public static JSONObject getJsonObject(List<String> headers) {
        // 获取信息
        JSONObject jsonObject1 = new JSONObject();
        for (String header : headers) {
            // 找到第一个分号的位置
            int endIndex = header.indexOf(";");
            String result = header.substring(0, endIndex);
            System.out.println("裁剪后header是：" + result);
            String[] pairs = result.split("=");
            jsonObject1.put(pairs[0], pairs[1]);
        }
        System.out.println("装变成json的数据：" + jsonObject1.toString());
        return jsonObject1;
    }

    @NotNull
    public static List<CourseVo> getCourseVos(String responseData) {
        // 定义正则表达式
        String regex = "course_(\\d+)_(\\d+)";

        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(responseData);

        List<CourseVo> arrayList = new ArrayList<>();
        // 查找匹配项
        while (matcher.find()) {
//                String courseId = matcher.group(1);
//                String clazzId = matcher.group(2);
            CourseVo courseVo = new CourseVo();
            courseVo.setCourseId(matcher.group(1));
            courseVo.setClazzId(matcher.group(2));
            arrayList.add(courseVo);
//                System.out.println("courseId: " + courseId);
//                System.out.println("clazzId: " + clazzId);
        }
        return arrayList;
    }

    /**
     * 解码网络gzip包
     * @param compressed
     * @return
     * @throws IOException
     */
    public static String decompress(byte[] compressed) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(compressed));
        BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }
}
