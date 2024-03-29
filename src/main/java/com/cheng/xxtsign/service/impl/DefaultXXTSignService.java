package com.cheng.xxtsign.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cheng.xxtsign.service.XXTSignService;
import com.cheng.xxtsign.utils.HeadersUtils;
import com.cheng.xxtsign.vo.CourseVo;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DefaultXXTSignService implements XXTSignService {

    @Override
    public String generalGroupSign(String mark, String location) {
        if (!HeadersUtils.hasJsonFile(mark)) {
            return "没有组";
        }
        JSONArray storeUserJoinGroup = HeadersUtils.getStoreUserJoinGroup(mark);
        if (storeUserJoinGroup.isEmpty()) {
            return "没有用户";
        }

        if (!ObjectUtil.isEmpty(storeUserJoinGroup)) {
            boolean b = false;
            int sign = 0;
            List<JSONObject> jsonObjects = null;
            // 查出所有用户
            for (int i = 0; i < storeUserJoinGroup.size(); i++) {

                // 现在是一个用户
                JSONObject obj = HeadersUtils.getUser(storeUserJoinGroup.getJSONObject(i).getString("phone"));
                JSONObject userAll = HeadersUtils.getUserAll(storeUserJoinGroup.getJSONObject(i).getString("phone"));
                String phone = userAll.getString("phone");
                String uSName = userAll.getString("U_SName");

                //
                DefaultXXTUserServiceImpl defaultXXTUserService = new DefaultXXTUserServiceImpl();
                if (sign == 0) {
                    // 查课程
                    List<CourseVo> courses = defaultXXTUserService.getCourses(obj.getString("_uid"),
                            obj.getString("_d"), obj.getString("vc3"));
                    jsonObjects = defaultXXTUserService.traverseCourseActivity(courses, obj);
                    sign++;
                }
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

                    }
                    else if (jsonObject.getString("otherId").equals("4")) {
                        // 位置
                        if (StringUtils.isEmpty(location)) {
                            b = defaultXXTUserService.locationSign(obj, jsonObject.getString("activeId"), uSName);
                        }else {
                            b = defaultXXTUserService.locationSign(obj, jsonObject.getString("activeId"), uSName, location);
                        }
                    }
                    else if (jsonObject.getString("otherId").equals("5")) {
                        // 签到码
                        String signCode = getSignCode(jsonObject.getString("activeId"));

                        b = defaultXXTUserService.generalSign(obj, jsonObject.getString("activeId"), uSName, signCode);
                    }

//                    if (b) return "签到失败，暂时不支持";
                }

            }

            if (b) return "签到成功";
        }


        return "签到失败";
    }


    @Override
    public String generalSign(String phone, String location) {
        boolean b = false;
        // 现在是一个用户
        JSONObject obj = HeadersUtils.getUser(phone);
        JSONObject userAll = HeadersUtils.getUserAll(phone);
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
        Map<String, String> headers = HeadersUtils.getHeaders();
        headers.put("Referer", "http://x.chaoxing.com/");
        headers.put("Proxy-Connection", "keep-alive");
        headers.put("Connection", "None");
        headers.put("Sec-Fetch-Dest", "None");
        headers.put("Sec-Fetch-Mode", "None");
        headers.put("Sec-Fetch-Site", "None");

        Response response = HeadersUtils.requestToXXT(url, "GET", headers);

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
