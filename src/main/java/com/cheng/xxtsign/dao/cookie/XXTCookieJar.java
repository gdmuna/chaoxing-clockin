package com.cheng.xxtsign.dao.cookie;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XXTCookieJar implements CookieJar {

    // 在这里定义你的持久化Cookie存储方式，比如SharedPreferences或者数据库
    // 这里使用一个简单的HashMap来保存Cookie
    private final Map<String, List<Cookie>> cookieStore = new HashMap<>();


    @NotNull
    @Override
    public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
        return null;
    }

    @Override
    public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {

    }
}
