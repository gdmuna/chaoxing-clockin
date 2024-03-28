package com.cheng.xxtsign.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {

    // 默认线程池使用了4线程
    public static ExecutorService pool = Executors.newFixedThreadPool(4);
}
