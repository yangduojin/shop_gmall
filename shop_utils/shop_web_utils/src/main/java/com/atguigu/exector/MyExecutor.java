package com.atguigu.exector;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyExecutor {
    //创建线程池
    private static ThreadPoolExecutor executor=new ThreadPoolExecutor(
            50, 500, 30,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100)
    );
    private MyExecutor(){}
    public static ThreadPoolExecutor getInstance()  {
        return executor;
    }
}