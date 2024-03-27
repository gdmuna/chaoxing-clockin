package com.cheng.xxtsign.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import com.cheng.xxtsign.dao.cookie.XXTCookieJar;
import com.cheng.xxtsign.service.XXTUserService;
import com.cheng.xxtsign.utils.HeadersUtils;
import com.cheng.xxtsign.vo.CourseVo;
import com.cheng.xxtsign.vo.UserLoginVo;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static cn.hutool.core.util.URLUtil.url;

@Service
public class DefaultXXTUserServiceImpl implements XXTUserService {

    @Value("${xxt.user.loginUrl}")
    private String loginUrl;
    @Value("${xxt.user.phoneSecret}")
    private String phoneSecret;
    @Value("${xxt.user.refer}")
    private String userDataRefer;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    public boolean userLogin(String phone, String password) {
        if (StringUtils.isEmpty(phone) && StringUtils.isEmpty(password)) {
            return false;
        }

//        RestTemplate restTemplate = new RestTemplate();
//
//        // 请求头
//        HttpHeaders httpHeaders = customRequestHeader();
//
        // 请求体
        String encryptPhone = HeadersUtils.encrypt(phone, phoneSecret, phoneSecret);
        String encryptPassword = HeadersUtils.encrypt(password, phoneSecret, phoneSecret);

        UserLoginVo userLoginVo = new UserLoginVo();
        userLoginVo.setUname(encryptPhone);
        userLoginVo.setPassword(encryptPassword);
        userLoginVo.setRefer(userDataRefer);
//
//
//        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
//
//        // 发送带有请求头的POST请求
//        ResponseEntity<String> response1 = restTemplate.exchange(loginUrl, HttpMethod.POST, entity, String.class);


        OkHttpClient client = new OkHttpClient();


        // 创建一个CookieJar来保存会话信息
        XXTCookieJar xxtCookieJar = new XXTCookieJar();

        client = client.newBuilder()
//                .cookieJar(xxtCookieJar)
                .build();

        // 构建一个POST请求，添加数据
//        RequestBody requestBody = RequestBody.create(JSONObject.toJSONString(userLoginVo), JSON);

        RequestBody requestBody = new FormBody.Builder()
                .add("fid", userLoginVo.getFid().toString())
                .add("uname", userLoginVo.getUname())
                .add("password", userLoginVo.getPassword())
                .add("refer", userLoginVo.getRefer())
                .add("t", userLoginVo.getT())
                .add("forbidotherlogin", userLoginVo.getForbidotherlogin().toString())
                .add("validate", "")
                .add("doubleFactorLogin", userLoginVo.getDoubleFactorLogin().toString())
                .add("independentId", userLoginVo.getIndependentId().toString())
                .add("independentNameId", userLoginVo.getIndependentNameId().toString())
                .build();

        Request.Builder builder = new Request.Builder();

        Map<String, String> stringStringMap = customRequestHeader();

        for (Map.Entry<String, String> entry : stringStringMap.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }

        Request request = builder
                .url(loginUrl)
                .post(requestBody)
                .build();


        try {
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            System.out.println("Response: " + responseBody);

            JSONObject jsonObject = JSONObject.parseObject(responseBody);
            if(jsonObject.getString("status").equals("true")) {
                // 登录请求成功
                List<String> headers = response.headers("set-Cookie");

                // 空置判断

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

                // 保存数据到本地
                HeadersUtils.storeUser(phone, jsonObject1);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }



        return true;
    }




    /**
     * 设置请求头，返回登录请求头
     * @return
     */
    public Map<String, String> customRequestHeader() {
//        HttpHeaders httpHeader = new HttpHeaders();


        Map<String, String> headers = HeadersUtils.getHeaders();
        Map<String, String> copyMap = new HashMap<>();
        copyMap.putAll(headers);

        copyMap.put("Accept", "application/json, text/javascript, */*; q=0.01");
        copyMap.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        copyMap.put("Origin", "https://passport2.chaoxing.com"); // TODO: 链接
        copyMap.put("Referer", "https://passport2.chaoxing.com/login?refer=https%3A%2F%2Fmooc2-ans.chaoxing.com%2Fmooc2-ans%2Fvisit%2Finteraction&fid=0&newversion=true&_blank=0");
        copyMap.put("Sec-Fetch-Dest", "empty");
        copyMap.put("Sec-Fetch-Mode", "cors");
        copyMap.put("Sec-Fetch-Site", "same-origin");
        copyMap.put("X-Requested-With", "XMLHttpRequest");

        // 设置请求头
//        for (Map.Entry<String, String> entry : copyMap.entrySet()) {
//            httpHeader.set(entry.getKey(), entry.getValue());
//        }

        return copyMap;
    }


    /**
     * 获取全部课程
     */

    public List<CourseVo> getCourses(String uid, String d, String v3){
        OkHttpClient client = new OkHttpClient();

        client = client.newBuilder().build();

        RequestBody requestBody = new FormBody.Builder()
                .add("courseType", "1")
                .add("courseFolderId", "0")
                .add("courseFolderSize", "0")
                .build();

        Request.Builder builder = new Request.Builder();

//        Map<String, String> stringStringMap = HeadersUtils.getHeaders();
//
//        Map<String, String> copyMap = new HashMap<>();
////        copyMap.putAll(stringStringMap);
//
//        copyMap.put("Accept", "text/html, */*; q=0.01");
//        copyMap.put("Accept-Encoding", "gzip, deflate");
//        copyMap.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
//        copyMap.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8;");
//        copyMap.put("Cookie", "_uid="+uid + "; _d=" + d + "; vc3=" + v3);
//
//        for (Map.Entry<String, String> entry : copyMap.entrySet()) {
//            builder.addHeader(entry.getKey(), entry.getValue());
//        }

        builder.addHeader("Accept", "text/html, */*; q=0.01")
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("Cookie", "_uid=" + uid + "; _d=" + d + "; vc3=" + v3);


        Request request = builder
                .url("https://mooc1-1.chaoxing.com/visit/courselistdata")
                .post(requestBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();
            String responseData = null;
            if (responseBody != null) {
                responseData = decompress(responseBody.bytes());
//                System.out.println("Decompressed Response Data: " + responseData);
            }

            // 从 HTML 页面内容，解析出所有 courseId 和 classId，填充到数组返回

//            int endOfCourseId;
//            int i = 1;
//            while (true) {
//                i = responseData.indexOf("course_", i);
//                if (i == -1) break;
//                endOfCourseId = responseData.indexOf("_", i + 7);
////                arr.add(new CourseType(
////                        data.substring(i + 7, endOfCourseId),
////                        data.substring(endOfCourseId + 1, data.indexOf("\"", i + 1))
////                ));
//                System.out.println("courseId" + responseData.substring(i + 7, endOfCourseId));
//                System.out.println("clazzId" + responseData.substring(endOfCourseId + 1, responseData.indexOf("\"", i + 1)));
//            }

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

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 解码网络gzip包
     * @param compressed
     * @return
     * @throws IOException
     */
    private static String decompress(byte[] compressed) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(compressed));
        BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }

    private JSONObject traverseCourseActivity(List<CourseVo> courseVoList, JSONObject cookie) {
        System.out.println("================正在查询是否有签到====================");
        // 只查3个
        int signNum = 0;
        // todo: 多线程处理, 目前只查一个签到
        for (CourseVo courseVo : courseVoList) {
            JSONObject activity = getActivity(courseVo, cookie);
            if (activity != null) {
                signNum++;
                return activity;
            }
        }
        return null;
    }

    private JSONObject getActivity(CourseVo courseVo, JSONObject cookie) {
        // get
        String url = "https://mobilelearn.chaoxing.com/v2/apis/active/student/activelist?fid=0&courseId="
                + courseVo.getCourseId() + "&classId=" + courseVo.getClazzId() + "&showNotStartedActive=0" + "&_=" + System.currentTimeMillis();

        System.out.println("请求的url：" + url);


        OkHttpClient client = new OkHttpClient();

        client = client.newBuilder().build();


        Request.Builder builder = new Request.Builder();


        builder.addHeader("Accept", "*/*")
                .addHeader("Referer", "https://mooc2-ans.chaoxing.com/")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Cookie", HeadersUtils.jsonToHeader(cookie));


        Request request = builder
                .url(url)
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();
//            String string = response.body().string();
//
//            System.out.println("查询结果" + string);
            String string = response.body().string();
            JSONObject jsonData = JSONObject.parseObject(string);

            JSONObject data = jsonData.getJSONObject("data");

            if (data.getJSONArray("activeList").size() != 0) {
                JSONObject activeList = data.getJSONArray("activeList").getJSONObject(0);
                String otherId = activeList.getString("otherId");

                if (isNumeric(otherId) && Integer.parseInt(otherId) >= 0 && Integer.parseInt(otherId) <= 5 && activeList.getIntValue("status") == 1) {
                    long currentTime = new Date().getTime();
                    long startTime = activeList.getLongValue("startTime");

                    if ((currentTime - startTime) / 1000 < 7200) {
                        // todo: 超过5个不签到
                        System.out.println("检测到活动：" + activeList.getString("nameOne"));

                        // todo: 抽取为对象
                        JSONObject result = new JSONObject();
                        result.put("activeId", activeList.getLongValue("id"));
                        result.put("name", activeList.getString("nameOne"));
                        result.put("courseId", courseVo.getCourseId());
                        result.put("classId", courseVo.getClazzId());
                        result.put("otherId", otherId);

                        System.out.println(result.toJSONString());

                        return result;
                    }
                }
            }


            System.out.println("========================================");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public static boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 获取用户的名字
     * @param cookie
     * @return
     */
    public String getUserInfo(JSONObject cookie) {
        // get
        String url = "https://passport2.chaoxing.com/mooc/accountManage";


        OkHttpClient client = new OkHttpClient();
        client = client.newBuilder().build();
        Request.Builder builder = new Request.Builder();
        builder.addHeader("Cookie", HeadersUtils.jsonToHeader(cookie));

        Request request = builder
                .url(url)
                .get()
                .build();


        try {
            Response response = client.newCall(request).execute();
            String data = response.body().string();

            int endOfMessageName = data.indexOf("messageName") + 20;
            String name = data.substring(endOfMessageName, data.indexOf("\"", endOfMessageName));

            System.out.println("用户个人信息：" + name);

            return name;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * 预签到
     * @param activity 需要签到的课程数据
     * @param cookie
     */
    public void preSign(JSONObject activity, JSONObject cookie) {
        // get
        String url = "https://mobilelearn.chaoxing.com/newsign/preSign?courseId=" + activity.getString("classId") +
                "&activePrimaryId=" + activity.getString("activeId") + "&general=1&sys=1&ls=1&appType=15&&tid=&uid=" +
                cookie.getString("_uid") + "&ut=s";

        OkHttpClient client = new OkHttpClient();
        client = client.newBuilder().build();
        Request.Builder builder = new Request.Builder();
        builder.addHeader("Cookie", HeadersUtils.jsonToHeader(cookie));

        Request request = builder
                .url(url)
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();

            analysisResult(activity, cookie);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void analysisResult(JSONObject activity, JSONObject cookie) {
        //get
        String url = "https://mobilelearn.chaoxing.com/pptSign/analysis?vs=1&DB_STRATEGY=RANDOM&aid=" + activity.getString("activeId");

        OkHttpClient client = new OkHttpClient();
        client = client.newBuilder().build();
        Request.Builder builder = new Request.Builder();
        builder.addHeader("Cookie", HeadersUtils.jsonToHeader(cookie));

        Request request = builder
                .url(url)
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();
            String code = response.body().string();

//            JSONObject jsonData = JSONObject.parseObject(string);
//
//            JSONObject data = jsonData.getJSONObject("data");
//            String code = data.toString();

            int codeStart = code.indexOf("code=\\'+\\'") + 8;
            String codeSubstring = code.substring(codeStart);

            int codeEnd = codeSubstring.indexOf("'");
            code = codeSubstring.substring(0, codeEnd);

            analysis2Result(code, cookie);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void analysis2Result(String code, JSONObject cookie) {
        //get
        String url = "https://mobilelearn.chaoxing.com/pptSign/analysis2?DB_STRATEGY=RANDOM&code=" + code;

        OkHttpClient client = new OkHttpClient();
        client = client.newBuilder().build();
        Request.Builder builder = new Request.Builder();
        builder.addHeader("Cookie", HeadersUtils.jsonToHeader(cookie));

        Request request = builder
                .url(url)
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();
            String string = response.body().string();
            System.out.println("请求结果：" + string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Sleep for 500ms
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void generalSign(JSONObject cookie, String activeId, String name) {
        // get
        String url = "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?activeId=" + activeId + "&uid=" + cookie.getString("_uid") +
                "&clientip=&latitude=-1&longitude=-1&appType=15&fid=" + cookie.getString("fid") + "&name=" + name;

        OkHttpClient client = new OkHttpClient();
        client = client.newBuilder().build();
        Request.Builder builder = new Request.Builder();
        builder.addHeader("Cookie", HeadersUtils.jsonToHeader(cookie));

        Request request = builder
                .url(url)
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();
            String string = response.body().string();
            System.out.println("签到结果：" + string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void locationSign(JSONObject cookie, String activeId, String name) {

        String longitude = randomValue(113.868804, 113.868810);
        String latitude = randomValue(22.930029, 22.930031);

        String url = "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?name=" + name + "&address=中国广东省东莞市大岭山镇科苑路"
                + "&activeId=" + activeId + "&uid=" + cookie.getString("_uid") + "&clientip=&latitude=" + latitude +
                "&longitude=" + longitude + "&fid=" + cookie.getString("fid") + "&appType=15&ifTiJiao=1&validate=";


        OkHttpClient client = new OkHttpClient();
        client = client.newBuilder().build();
        Request.Builder builder = new Request.Builder();
        builder.addHeader("Cookie", HeadersUtils.jsonToHeader(cookie));

        Request request = builder
                .url(url)
                .get()
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
            String string = response.body().string();
            System.out.println("签到结果：" + string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static String randomValue(double min, double max) {
        Random random = new Random();
        double randomValue = min + (max - min) * random.nextDouble();

        // 控制小数点后的位数为6位
        DecimalFormat df = new DecimalFormat("#.######");
        String randomString = df.format(randomValue);

        System.out.println("随机：" + randomString);

        return randomString;
    }


    public static void main(String[] args) {
        JSONObject user = HeadersUtils.getUser("13018436372");
        DefaultXXTUserServiceImpl defaultXXTUserService = new DefaultXXTUserServiceImpl();
        List<CourseVo> courses = defaultXXTUserService.getCourses(user.getString("_uid"), user.getString("_d"), user.getString("vc3"));

        JSONObject jsonObject = defaultXXTUserService.traverseCourseActivity(courses, user);

        if (ObjectUtil.isEmpty(jsonObject)) {
            return;
        }
        String userInfo = defaultXXTUserService.getUserInfo(user);


        defaultXXTUserService.preSign(jsonObject, user);

        // 普通签到
        if (jsonObject.getString("otherId").equals("0")) {
            defaultXXTUserService.generalSign(user, jsonObject.getString("activeId"), userInfo);
        } else if (jsonObject.getString("otherId").equals("4")) {
            defaultXXTUserService.locationSign(user, jsonObject.getString("activeId"), userInfo);
        }


//        DefaultXXTUserServiceImpl.randomValue(113.868562, 113.869482);
    }

}
