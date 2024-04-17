package com.cheng.xxtsign.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cheng.xxtsign.exception.user.XXTUserException;
import com.cheng.xxtsign.mapper.XXTUserMapper;
import com.cheng.xxtsign.service.XXTSignService;
import com.cheng.xxtsign.utils.XXTHttpRequestUtils;
import com.cheng.xxtsign.dao.vo.CourseVo;
import lombok.extern.java.Log;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
@Service
public class DefaultXXTSignService implements XXTSignService {

    @Autowired
    private XXTUserMapper xxtUserMapper;
    @Autowired
    private DefaultXXTUserServiceImpl xxtUserService;

    @Override
    public List<String> generalGroupSign(String mark, String location) {
        if (!XXTHttpRequestUtils.hasJsonFile(mark)) {
            throw new XXTUserException("没有此组");
        }

        JSONArray userListByMark = xxtUserMapper.getUserListByMark(mark);
        if (userListByMark.isEmpty()) {
            throw new XXTUserException("此组没有用户");
        }

        List<String> resList = new ArrayList<>();

        /**
         * 1. 查出用户缓存
         * 2. 查询所有的课程
         * 3. 需要签的课程对应的签到类型
         */
        if (!ObjectUtil.isEmpty(userListByMark)) {
            // 是否查询出第一个用户需要签到的课程
            boolean flat = false;
            List<JSONObject> needSignCourse = null;

            // 获取单个用户 todo:更新为异步
            for (int i = 0; i < userListByMark.size(); i++) {
                boolean isSign = false;

                // todo: 写mapper里面
                // 1. 查出用户缓存
                // user是移除自己添加的字段,原cookie
                JSONObject user = XXTHttpRequestUtils.getUser(userListByMark.getJSONObject(i).getString("phone"));
                // userAll是全部字段
                JSONObject userAll = XXTHttpRequestUtils.getUserAll(userListByMark.getJSONObject(i).getString("phone"));
                // 用户名
                String uSName = userAll.getString("U_SName");
                log.info("用户："+ uSName + "->开始签到");

                if (!flat) {
                    // 2. 查询所有的课程
                    List<CourseVo> course = getCourse(user.getString("_uid"),
                            user.getString("_d"), user.getString("vc3"));
                    if (course == null) {
                        throw new XXTUserException("读取到用户课程为空");
                    }
                    needSignCourse = getNeedSignCourse(course, user);
                    flat = true;
                }
                if (needSignCourse.isEmpty()) {
                    throw new XXTUserException("没有需要签到的课");
                }

                // 3. 需要签的课程对应的签到类型
                for (JSONObject signCourse : needSignCourse) {
                    // todo:转移代码到此service,签到失败标记
                    xxtUserService.preSign(signCourse, user);

                    if (signCourse.getString("otherId").equals("0")) {
                        // 普通
                        isSign = xxtUserService.generalSign(user, signCourse.getString("activeId"), uSName, null);
                    } else if (signCourse.getString("otherId").equals("3")) {
                        // 手势
                        String signCode = getSignCode(signCourse.getString("activeId"));
                        isSign = xxtUserService.generalSign(user, signCourse.getString("activeId"), uSName, signCode);
                    }
                    else if (signCourse.getString("otherId").equals("4")) {
                        // 位置
                        if (StringUtils.isEmpty(location)) {
                            isSign = xxtUserService.locationSign(user, signCourse.getString("activeId"), uSName);
                        }else {
                            isSign = xxtUserService.locationSign(user, signCourse.getString("activeId"), uSName, location);
                        }
                    }
                    else if (signCourse.getString("otherId").equals("5")) {
                        // 签到码
                        String signCode = getSignCode(signCourse.getString("activeId"));
                        isSign = xxtUserService.generalSign(user, signCourse.getString("activeId"), uSName, signCode);
                    } else {
                        log.info("未知类型签到");
                    }
                }

                if (!isSign) {
                    resList.add("用户：" + uSName + "签到失败 " + "联系方式：" + userAll.getString("phone"));
                }else {
                    resList.add("用户：" + uSName + "签到成功 " + "联系方式：" + userAll.getString("phone"));
                }
            }
        }

        return resList;
    }

    /**
     * 获取需要签到的课程
     * @param courseVoList 用户的课程列表
     * @param cookie 用户登录返回的cookie
     */
    private List<JSONObject> getNeedSignCourse(List<CourseVo> courseVoList, JSONObject cookie) {

        log.info("====================正在查询签到====================");
        List<JSONObject> courses = new ArrayList<>();
        // 只查3个
        int signNum = 0;
        // todo: 多线程处理，太多课程了，得加延时或者只查前面的
        for (CourseVo courseVo : courseVoList) {
            // 多于3课程需要签到退出
            if (signNum >= 3) {
                break;
            }

            JSONObject activity = getActivity(courseVo, cookie);
            if (activity != null) {
                signNum++;
                courses.add(activity);
            }
        }

        log.info("=================查询签到结束=======================");
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

        Response response = null;
        try {
            response = XXTHttpRequestUtils.requestToXXT(url, "GET", headerMap);
            String responseString = response.body().string();

            return getCourseOtherActivity(courseVo, responseString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(response != null) {
                response.close();
            }
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
//                    System.out.println("检测到活动：" + activeList.getString("nameOne"));

                    // todo: 抽取为对象
                    JSONObject result = new JSONObject();
                    result.put("activeId", activeList.getLongValue("id"));
                    result.put("name", activeList.getString("nameOne"));
                    result.put("courseId", courseVo.getCourseId());
                    result.put("classId", courseVo.getClazzId());
                    result.put("otherId", otherId);

//                    System.out.println(result.toJSONString());

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
     * 获取用户的课程
     * @param uid
     * @param d
     * @param vc3
     * @return
     */
    private List<CourseVo> getCourse(String uid, String d, String vc3) {
        String url = "https://mooc1-1.chaoxing.com/visit/courselistdata";

        // 请求头
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Accept", "text/html, */*; q=0.01");
        headerMap.put("Accept-Encoding", "gzip, deflate");
        headerMap.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
        headerMap.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        headerMap.put("Cookie", "_uid=" + uid + "; _d=" + d + "; vc3=" + vc3);

        // 请求体
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("courseType", "1");
        paramMap.put("courseFolderId", "0");
        paramMap.put("courseFolderSize", "0");

        Response response = null;
        try {
            response = XXTHttpRequestUtils.requestToXXT(url, "POST", headerMap, paramMap);
            ResponseBody responseBody = response.body();
            String responseData = null;

            if (responseBody != null) {
                responseData = XXTHttpRequestUtils.decompress(responseBody.bytes());
            }

            return XXTHttpRequestUtils.getCourseVos(responseData);

        } catch (IOException e) {
            e.printStackTrace();
            throw new XXTUserException("在获取课程的时候发生了错误");
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    @Override
    public String generalSign(String phone, String location) {
        boolean b = false;
        // 现在是一个用户
        JSONObject obj = XXTHttpRequestUtils.getUser(phone);
        JSONObject userAll = XXTHttpRequestUtils.getUserAll(phone);
        String uSName = userAll.getString("U_SName");


        //
        DefaultXXTUserServiceImpl defaultXXTUserService = new DefaultXXTUserServiceImpl();
        // 查课程
        List<CourseVo> courses = defaultXXTUserService.getCourses(obj.getString("_uid"),
                obj.getString("_d"), obj.getString("vc3"));
        List<JSONObject> jsonObjects = defaultXXTUserService.traverseCourseActivity(courses, obj);

        if (jsonObjects.isEmpty()) {
            return "没有需要签到的课";
        }


        // 一个用户的全部签到
        for (JSONObject jsonObject : jsonObjects) {
            if (ObjectUtil.isEmpty(jsonObject)) {
                return "签到失败， 需要签到的课程为空";
            }
            defaultXXTUserService.preSign(jsonObject, obj);


            if (jsonObject.getString("otherId").equals("0")) {
                // 普通
                b = defaultXXTUserService.generalSign(obj, jsonObject.getString("activeId"), uSName, null);

            } else if (jsonObject.getString("otherId").equals("3")) {
                // 手势
                String signCode = getSignCode(jsonObject.getString("activeId"));

                b = defaultXXTUserService.generalSign(obj, jsonObject.getString("activeId"), uSName, signCode);

            } else if (jsonObject.getString("otherId").equals("4")) {
                // 位置
                if (StringUtils.isEmpty(location)) {
                    b = defaultXXTUserService.locationSign(obj, jsonObject.getString("activeId"), uSName);
                }else {
                    b = defaultXXTUserService.locationSign(obj, jsonObject.getString("activeId"), uSName, location);
                }
            } else if (jsonObject.getString("otherId").equals("5")) {
                // 签到码
                String signCode = getSignCode(jsonObject.getString("activeId"));

                b = defaultXXTUserService.generalSign(obj, jsonObject.getString("activeId"), uSName, signCode);
            }

        }

        if (b) {
            return "签到成功";
        }

        return "发送未知错误";
    }


    /**
     * 获取签到码
     */
    public static String getSignCode(String activeId) {
        String url = "https://mobilelearn.chaoxing.com/widget/sign/pcTeaSignController/showSignInfo?activeId=" + activeId;
        Map<String, String> headers = XXTHttpRequestUtils.getHeaders();
        headers.put("Referer", "http://x.chaoxing.com/");
        headers.put("Proxy-Connection", "keep-alive");
        headers.put("Connection", "None");
        headers.put("Sec-Fetch-Dest", "None");
        headers.put("Sec-Fetch-Mode", "None");
        headers.put("Sec-Fetch-Site", "None");

        Response response = XXTHttpRequestUtils.requestToXXT(url, "GET", headers);

        try {
            String string = response.body().string();

            String regex = "id=\"signCode\" value=\"(.*?)\"";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(string);

            if (matcher.find()) {
                String signCodeValue = matcher.group(1);
                System.out.println("请求获取验证码: " + signCodeValue);
                return signCodeValue;
            } else {
                System.out.println("没有验证码");
                return "";
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 手势和签到码
     */

    public static String codeSign(JSONObject cookie, String activeId, String name, String signCode) {
        String url = "https://mobilelearn.chaoxing.com/v2/apis/sign/signIn?activeId=" + activeId + "&signCode" + signCode
                + "&validate=";
        return null;
    }
}
