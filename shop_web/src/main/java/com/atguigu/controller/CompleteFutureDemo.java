package com.atguigu.controller;

import java.util.concurrent.CompletableFuture;

public class CompleteFutureDemo {
    public static void main(String[] args) throws Exception {
        runYx();
        System.out.println("yx");
        supplyAsync();
    }
    public static void runYx() {
        CompletableFuture.runAsync(() -> {
            System.out.println("hello");
            int i = 10 / 0;
        }).whenComplete((Void acceptVal,Throwable throwable)->{
            System.out.println("获取上面执行之后的返回值");
            System.out.println("whenComplete接受到上面发生的异常");
        }).exceptionally((Throwable throwable)->{
            System.out.println(throwable);
            return null;
        });
    }

    public static void supplyAsync() throws Exception {
        CompletableFuture<String> supplyAsync = CompletableFuture.supplyAsync(() ->{
            return "hello supplyAsync" ;
        });
        //其实这个方法是一个阻塞方法
        System.out.println(Thread.currentThread().getName()+supplyAsync.get());

    }
}
