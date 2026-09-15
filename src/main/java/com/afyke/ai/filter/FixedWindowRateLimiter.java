package com.afyke.ai.filter;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// 交给 Spring 管理为单例组件；Filter 会注入并复用这一份限流计数器。
@Component
public class FixedWindowRateLimiter {

    // 一个用户在一个 60 秒固定窗口内最多可通过 5 次请求。
    private static final int LIMIT_PER_MINUTE = 5;
    private static final long WINDOW_MILLIS = 60_000L;

    // key 是用户标识，value 保存“窗口从何时开始”和“本窗口已用了几次”。
    // ConcurrentHashMap 允许多个 HTTP 请求线程同时访问不同用户的计数。
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public boolean allow(String key) {
        long now = System.currentTimeMillis();
        // compute 让同一用户键的“创建窗口 / 增加计数”成为一次原子更新。
        Counter counter = counters.compute(key, (ignored, old) -> {
            if (old == null || now - old.windowStartedAt() >= WINDOW_MILLIS) {
                // 没有旧窗口或窗口已过期：从本次请求开始新的计数。
                return new Counter(now, new AtomicInteger(1));
            }
            // 仍在当前窗口：记录这一次访问。
            old.used().incrementAndGet();
            return old;
        });
        // 第 1 至第 5 次允许；第 6 次及以后拒绝。
        return counter.used().get() <= LIMIT_PER_MINUTE;
    }

    // windowStartedAt 用于判断 60 秒是否结束；AtomicInteger 避免并发 ++ 时丢失计数。
    private record Counter(long windowStartedAt, AtomicInteger used) {
    }
}
