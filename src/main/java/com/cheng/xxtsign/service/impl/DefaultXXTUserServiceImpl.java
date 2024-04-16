package com.cheng.xxtsign.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cheng.xxtsign.dao.vo.XXTUserVO;
import com.cheng.xxtsign.enums.LocationSignEnum;
import com.cheng.xxtsign.exception.user.XXTUserException;
import com.cheng.xxtsign.mapper.XXTUserMapper;
import com.cheng.xxtsign.service.XXTUserService;
import com.cheng.xxtsign.utils.XXTHttpRequestUtils;
import com.cheng.xxtsign.dao.vo.CourseVo;
import com.cheng.xxtsign.dao.vo.UserLoginVo;
import lombok.extern.java.Log;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Service
@Log
public class DefaultXXTUserServiceImpl implements XXTUserService {

    @Value("${xxt.user.loginUrl}")
    private String loginUrl;
    @Value("${xxt.user.phoneSecret}")
    private String phoneSecret;
    @Value("${xxt.user.refer}")
    private String userDataRefer;

    @Autowired
    private XXTUserMapper xxtUserMapper;

//    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    public boolean userLogin(String phone, String password) {
        if (StringUtils.isEmpty(phone) && StringUtils.isEmpty(password)) {
            throw new XXTUserException("电话号码或者密码为空");
        }

        // 请求体
        String encryptPhone = XXTHttpRequestUtils.encrypt(phone, phoneSecret, phoneSecret);
        String encryptPassword = XXTHttpRequestUtils.encrypt(password, phoneSecret, phoneSecret);

        UserLoginVo userLoginVo = new UserLoginVo();
        userLoginVo.setUname(encryptPhone);
        userLoginVo.setPassword(encryptPassword);
        userLoginVo.setRefer(userDataRefer);

        Response response = null;
        try {
            response = XXTHttpRequestUtils.requestToXXT(loginUrl, "POST", customRequestHeader(),
                    XXTHttpRequestUtils.objectToMap(userLoginVo));
            String responseBody = response.body().string();

            log.info(phone + " 登录Response: " + responseBody);

            JSONObject jsonObject = JSONObject.parseObject(responseBody);
            if(jsonObject.getString("status").equals("true")) {
                // 登录请求成功
                List<String> headers = response.headers("set-Cookie");
                // 空置判断
                JSONObject jsonObject1 = XXTHttpRequestUtils.getJsonObject(headers);
                // 保存数据到本地
                XXTHttpRequestUtils.storeUser(phone, jsonObject1);
                String userInfo = getUserInfo(jsonObject1);
                // 保存用户名
                XXTHttpRequestUtils.storeUserName(userInfo, phone);
            }else {
                // 登录失败
                throw new XXTUserException("登录失败，请重新登录（多次登录失败请联系开发者）");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new XXTUserException("登录失败，请重新登录（多次登录失败请联系开发者）");
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return true;
    }

    /**
     * 设置请求头
     * @return 登录请求头
     */
    public Map<String, String> customRequestHeader() {
        Map<String, String> headers = XXTHttpRequestUtils.getHeaders();
        Map<String, String> copyMap = new HashMap<>();
        copyMap.putAll(headers);

        copyMap.put("Accept", "application/json, text/javascript, */*; q=0.01");
        copyMap.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        // TODO: 链接
        copyMap.put("Origin", "https://passport2.chaoxing.com");
        copyMap.put("Referer", "https://passport2.chaoxing.com/login?refer=https%3A%2F%2Fmooc2-ans.chaoxing.com%2Fmooc2-ans%2Fvisit%2Finteraction&fid=0&newversion=true&_blank=0");
        copyMap.put("Sec-Fetch-Dest", "empty");
        copyMap.put("Sec-Fetch-Mode", "cors");
        copyMap.put("Sec-Fetch-Site", "same-origin");
        copyMap.put("X-Requested-With", "XMLHttpRequest");

        return copyMap;
    }


    /**
     * 获取全部课程
     */

    public List<CourseVo> getCourses(String uid, String d, String v3){
        String url = "https://mooc1-1.chaoxing.com/visit/courselistdata";

        // 请求头
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Accept", "text/html, */*; q=0.01");
        headerMap.put("Accept-Encoding", "gzip, deflate");
        headerMap.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
        headerMap.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        headerMap.put("Cookie", "_uid=" + uid + "; _d=" + d + "; vc3=" + v3);

        // 请求体
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("courseType", "1");
        paramMap.put("courseFolderId", "0");
        paramMap.put("courseFolderSize", "0");



        try {
            Response response = XXTHttpRequestUtils.requestToXXT(url, "POST", headerMap, paramMap);
            ResponseBody responseBody = response.body();
            String responseData = null;
            if (responseBody != null) {
                responseData = XXTHttpRequestUtils.decompress(responseBody.bytes());
            }

            return XXTHttpRequestUtils.getCourseVos(responseData);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 查询需要签到的课
     * @param courseVoList
     * @param cookie
     * @return
     */
    public List<JSONObject> traverseCourseActivity(List<CourseVo> courseVoList, JSONObject cookie) {
        System.out.println("================正在查询是否有签到====================");

        List<JSONObject> courses = new ArrayList<>();
        // 只查3个
        int signNum = 0;
        // todo: 多线程处理，太多课程了，得加延时或者只查前面的
        for (CourseVo courseVo : courseVoList) {
            if (signNum >= 3) {
                break;
            }
            JSONObject activity = getActivity(courseVo, cookie);
            if (activity != null) {
                signNum++;
                courses.add(activity);
            }
        }

        System.out.println("========================================");
        return courses;
    }

    /**
     * 查询签到
     * @param courseVo
     * @param cookie
     * @return
     */
    private JSONObject getActivity(CourseVo courseVo, JSONObject cookie) {
        // get
        String url = "https://mobilelearn.chaoxing.com/v2/apis/active/student/activelist?fid=0&courseId="
                + courseVo.getCourseId() + "&classId=" + courseVo.getClazzId() + "&showNotStartedActive=0"
                + "&_=" + System.currentTimeMillis();

        // 请求头
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("Accept", "*/*");
        headerMap.put("Referer", "https://mooc2-ans.chaoxing.com/");
        headerMap.put("Sec-Fetch-Dest", "empty");
        headerMap.put("Sec-Fetch-Mode", "cors");
        headerMap.put("Sec-Fetch-Site", "same-origin");
        headerMap.put("X-Requested-With", "XMLHttpRequest");
        headerMap.put("Cookie", XXTHttpRequestUtils.jsonToHeader(cookie));

        Response response = XXTHttpRequestUtils.requestToXXT(url, "GET", headerMap);

        try {
//            System.out.println("查询结果" + string);
            String responseString = response.body().string();

            return getCourseOtherActivity(courseVo, responseString);



        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查到活动
     * @param courseVo
     * @param responseString
     * @return
     */
    @Nullable
    private static JSONObject getCourseOtherActivity(CourseVo courseVo, String responseString) {
        JSONObject jsonData = JSONObject.parseObject(responseString);
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
     * @return 用户名
     */
    public String getUserInfo(JSONObject cookie) {
        // get todo:魔术值
        String url = "https://passport2.chaoxing.com/mooc/accountManage";
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("Cookie", XXTHttpRequestUtils.jsonToHeader(cookie));

        Response response = null;


        try {
            response = XXTHttpRequestUtils.requestToXXT(url, "GET", headerMap);
            String data = response.body().string();

            // 获取用户名字
            int endOfMessageName = data.indexOf("messageName") + 20;
            String name = data.substring(endOfMessageName, data.indexOf("\"", endOfMessageName));
            log.info("===================================");
            log.info("用户名字：" + name);
            log.info("===================================");

            return name;
        } catch (IOException e) {
            e.printStackTrace();
            throw new XXTUserException("登录成功，但是获取用户名失败，建议重新登录");
        } finally {
            if (response != null) {
                response.close();
            }
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
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("Cookie", XXTHttpRequestUtils.jsonToHeader(cookie));

        // 第一次预先请求
        XXTHttpRequestUtils.requestToXXT(url, "GET", headerMap);
        System.out.println("预先请求完成1");

        // 第二次
        analysisResult(activity, cookie);
    }

    public void analysisResult(JSONObject activity, JSONObject cookie) {
        //get
        String url = "https://mobilelearn.chaoxing.com/pptSign/analysis?vs=1&DB_STRATEGY=RANDOM&aid=" + activity.getString("activeId");

        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("Cookie", XXTHttpRequestUtils.jsonToHeader(cookie));

        // 第二次预先请求
        Response response = XXTHttpRequestUtils.requestToXXT(url, "GET", headerMap);
        System.out.println("预先请求完成2");

        try {
            String code = response.body().string();
//            JSONObject jsonData = JSONObject.parseObject(string);
//
//            JSONObject data = jsonData.getJSONObject("data");
//            String code = data.toString();

            // 正则出要的码
            int codeStart = code.indexOf("code=\\'+\\'") + 8;
            String codeSubstring = code.substring(codeStart);

            int codeEnd = codeSubstring.indexOf("'");
            code = codeSubstring.substring(0, codeEnd);

            // 再次请求
            analysis2Result(code, cookie);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void analysis2Result(String code, JSONObject cookie) {
        //get
        String url = "https://mobilelearn.chaoxing.com/pptSign/analysis2?DB_STRATEGY=RANDOM&code=" + code;

        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("Cookie", XXTHttpRequestUtils.jsonToHeader(cookie));

        // 第二次预先请求
        Response response = XXTHttpRequestUtils.requestToXXT(url, "GET", headerMap);
        System.out.println("预先请求完成3");

        try {
            String string = response.body().string();
            System.out.println("预先请求结果：" + string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Sleep for 500ms
        try {
            System.out.println("睡眠0.5秒");
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 普通签到
    public boolean generalSign(JSONObject cookie, String activeId, String name, String signCode) {
        // get
        String url;

        if (StringUtils.isEmpty(signCode)) {
            url = "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?activeId=" + activeId + "&uid=" + cookie.getString("_uid") +
                    "&clientip=&latitude=-1&longitude=-1&appType=15&fid=" + cookie.getString("fid") + "&name=" + name;
        }else {
            url = "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?activeId=" + activeId + "&signCode=" + signCode + "&uid=" + cookie.getString("_uid") +
                    "&clientip=&latitude=-1&longitude=-1&appType=15&fid=" + cookie.getString("fid") + "&name=" + name;
        }

        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("Cookie", XXTHttpRequestUtils.jsonToHeader(cookie));

        Response response = XXTHttpRequestUtils.requestToXXT(url, "GET", headerMap);


        try {
            String string = response.body().string();
            System.out.println("签到结果：" + string);
            if (string.equals("success")) {
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return false;
    }


    public boolean locationSign(JSONObject cookie, String activeId, String name) {

//        String longitude = randomValue(113.868804, 113.868810);
//        String latitude = randomValue(22.930029, 22.930031);
        return locationSign(cookie, activeId, name, null);

    }

    public boolean locationSign(JSONObject cookie, String activeId, String name, String location) {
        // 默认在A栋
        String longitude = randomCenterValue(LocationSignEnum.GDMU_A.getLongitude());
        String latitude = randomCenterValue(LocationSignEnum.GDMU_A.getLatitude());

        // 位置确定
        if (location != null) {
            if (location.equals("GDMUB")) {
                longitude = randomCenterValue(LocationSignEnum.GDMU_B.getLongitude());
                latitude = randomCenterValue(LocationSignEnum.GDMU_B.getLatitude());
            } else if (location.equals("GDMUC")) {
                longitude = randomCenterValue(LocationSignEnum.GDMU_C.getLongitude());
                latitude = randomCenterValue(LocationSignEnum.GDMU_C.getLatitude());
            } else if (location.equals("GDMUD")) {
                longitude = randomCenterValue(LocationSignEnum.GDMU_D.getLongitude());
                latitude = randomCenterValue(LocationSignEnum.GDMU_D.getLatitude());
            } else if (location.equals("GDMUE")) {
                longitude = randomCenterValue(LocationSignEnum.GDMU_E.getLongitude());
                latitude = randomCenterValue(LocationSignEnum.GDMU_E.getLatitude());
            } else if (location.equals("GDMUF")) {
                longitude = randomCenterValue(LocationSignEnum.GDMU_F.getLongitude());
                latitude = randomCenterValue(LocationSignEnum.GDMU_F.getLatitude());
            } else if (location.equals("GDMUG")) {
                longitude = randomCenterValue(LocationSignEnum.GDMU_G.getLongitude());
                latitude = randomCenterValue(LocationSignEnum.GDMU_G.getLatitude());
            } else if (location.equals("GDMUH")) {
                longitude = randomCenterValue(LocationSignEnum.GDMU_H.getLongitude());
                latitude = randomCenterValue(LocationSignEnum.GDMU_H.getLatitude());
            } else if (location.equals("S")) {
                longitude = randomCenterValue(LocationSignEnum.GDMU_S.getLongitude());
                latitude = randomCenterValue(LocationSignEnum.GDMU_S.getLatitude());
            }
        }

        String url = "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?name=" + name + "&address=中国广东省东莞市大岭山镇科苑路"
                + "&activeId=" + activeId + "&uid=" + cookie.getString("_uid") + "&clientip=&latitude=" + latitude +
                "&longitude=" + longitude + "&fid=" + cookie.getString("fid") + "&appType=15&ifTiJiao=1&validate=";

        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("Cookie", XXTHttpRequestUtils.jsonToHeader(cookie));
        Response response = XXTHttpRequestUtils.requestToXXT(url, "GET", headerMap);

        try {
            String string = response.body().string();
            System.out.println("签到结果：" + string);
            if (string.equals("success")) {
                return true;
            }
            return false;
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

    public static String randomCenterValue(double num) {
        Random random = new Random();
        double randomValue = (num - 0.000250) + (0.000500) * random.nextDouble();

        // 控制小数点后的位数为6位
        DecimalFormat df = new DecimalFormat("#.######");
        String randomString = df.format(randomValue);

        System.out.println("随机：" + randomString);

        return randomString;
    }

    /**
     * 用户加入组
     * @param mark 组的标识
     * @param phone 电话号码
     * @return
     */
    @Override
    public boolean join(String mark, String phone) {
        /**
         * 1. 先检查是否有这个组（有没有这个文件）
         * 2. 检查有没有这个用户
         * 3. 加入到文件中
         */
        if (StringUtils.isEmpty(phone) && StringUtils.isEmpty(mark)) {
            throw new XXTUserException("电话号和组标识不能为空");
        }
        // 1. 先检查是否有这个组（有没有这个文件）// 2. 检查有没有这个用户
        if (!XXTHttpRequestUtils.hasJsonFile(mark) || !XXTHttpRequestUtils.hasUser(phone)) {
            throw new XXTUserException("用户未登录或者不存在组");
        }
        // 3. 加入到文件中
        JSONObject user = xxtUserMapper.getUserByPhone(phone);
        int insertUserToGroup = xxtUserMapper.insertUserToGroup(user, mark);
        if (insertUserToGroup == 1) {
            return true;
        }

        return false;
    }


    @Override
    public List<XXTUserVO> getUserListByMark(String mark) {
        if (!XXTHttpRequestUtils.hasJsonFile(mark)) {
            throw new XXTUserException("不存在组");
        }
        // 获取组内用户名字和联系方式等
        List<XXTUserVO> xxtUserVOS = new ArrayList<>();
        JSONArray userListByMark = xxtUserMapper.getUserListByMark(mark);

        // 遍历 JSONArray 中的每个 JSON 对象，并将其转换为 XXTUserVO 对象
        for (int i = 0; i < userListByMark.size(); i++) {
            JSONObject jsonObject = userListByMark.getJSONObject(i);
            XXTUserVO xxtUserVO = new XXTUserVO();
            // todo: 魔术
            xxtUserVO.setName(jsonObject.getString("U_SName"));
            xxtUserVO.setPhone(jsonObject.getString("phone"));
            xxtUserVO.setLoginTime(jsonObject.getString("Login_Sign_System_Time"));
            xxtUserVO.setAgainLoginTime(jsonObject.getString("Login_Sign_System_Time"));
            xxtUserVOS.add(xxtUserVO);
        }

        return xxtUserVOS;
    }

    @Override
    public void addGroup(String mark, String au) {
        // todo: 魔术
        String AU = "cheng_admin";
        if (!au.equals(AU)) {
            throw new XXTUserException("你没有创建权限");
        }

        String filePath = mark + ".json";

        // 创建一个 File 对象
        File file = new File(filePath);

        // 检查文件是否存在
        if (file.exists()) {
            throw new XXTUserException("此组已经创建");
        } else {
            try {
                // 尝试创建空文件
                boolean created = file.createNewFile();

//                if (created) {
//                    System.out.println("Empty file created successfully: " + filePath);
//                } else {
//                    System.out.println("File could not be created.");
//                }
            } catch (IOException e) {
//                System.out.println("An error occurred while creating the file.");
                e.printStackTrace();
                throw new XXTUserException("创建组错误");
            }
        }
    }

    public static void main(String[] args) {
        JSONObject user = XXTHttpRequestUtils.getUser("15992601106");
        DefaultXXTUserServiceImpl defaultXXTUserService = new DefaultXXTUserServiceImpl();
        List<CourseVo> courses = defaultXXTUserService.getCourses(user.getString("_uid"), user.getString("_d"), user.getString("vc3"));

        for (JSONObject jsonObject : defaultXXTUserService.traverseCourseActivity(courses, user)) {
            if (ObjectUtil.isEmpty(jsonObject)) {
                return;
            }
            String userInfo = defaultXXTUserService.getUserInfo(user);


            defaultXXTUserService.preSign(jsonObject, user);

            // 普通签到
//            if (jsonObject.getString("otherId").equals("0")) {
//                defaultXXTUserService.generalSign(user, jsonObject.getString("activeId"), userInfo);
//            } else if (jsonObject.getString("otherId").equals("3")) {
//                defaultXXTUserService.generalSign(user, jsonObject.getString("activeId"), userInfo);
//            } else if (jsonObject.getString("otherId").equals("4")) {
//                defaultXXTUserService.locationSign(user, jsonObject.getString("activeId"), userInfo);
//            }
        }


        


//        DefaultXXTUserServiceImpl.randomValue(113.868562, 113.869482);


    }

}
