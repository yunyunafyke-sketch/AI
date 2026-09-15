package com.afyke.ai.test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一个 Counter 对象只保存一个用户的请求次数。
 */
public class Counter {

    // 这个用户的当前次数；初始为 0。
    // final 表示 count 始终指向同一个 AtomicInteger，不能中途换成另一个计数器；
    // 但仍然可以调用 incrementAndGet() 修改这个计数器内部的数字。
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * 让当前次数安全地加一，并返回加完后的次数。
     */
    public int increment() {
        return count.incrementAndGet();
    }

    /**
     * 读取当前累计次数。
     */
    public int get() {
        return count.get();
    }
}