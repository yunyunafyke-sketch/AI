package com.afyke.ai.ratelimit;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Component：把限流器交给 Spring 管理；默认是单例，所有请求共用这一份计数数据。
@Component
public class FixedWindowRateLimiter {

    // 每个用户在一个固定时间窗口内最多允许 5 次请求。
    private static final int LIMIT_PER_MINUTE = 5;
    // 一个窗口持续 60_000 毫秒，也就是 60 秒。
    private static final long WINDOW_MILLIS = 60_000L;

    // key 是用户 ID，value 保存该用户的窗口开始时间和当前窗口已请求次数。
    // ConcurrentHashMap：允许多个 HTTP 请求线程安全地同时更新不同用户的计数。
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    // allow：记录 key 对应用户的本次请求；允许返回 true，超过 5 次返回 false。
    public boolean allow(String key) {
        // 获取当前时间的毫秒值，用来判断旧窗口是否已经持续满 60 秒。
        long now = System.currentTimeMillis();

        // compute：针对当前用户 key 原子地执行“创建窗口或增加计数”，避免并发请求把计数覆盖掉。
        // ignored 是当前 key（这里用不到），old 是这个用户原来的 Counter；第一次请求时 old 为 null。
        Counter counter = counters.compute(key, (ignored, old) -> {
            if (old == null || now - old.windowStartedAt() >= WINDOW_MILLIS) {
                // 新用户或旧窗口已过期：从当前时刻创建新窗口，并把本次请求记为第 1 次。
                return new Counter(now, new AtomicInteger(1));
            }

            // 窗口尚未过期：把当前用户在本窗口的请求次数加 1。
            old.used().incrementAndGet();
            return old;
        });

        // 第 1～5 次返回 true；从第 6 次开始返回 false，直到 60 秒窗口过期后重新计数。
        return counter.used().get() <= LIMIT_PER_MINUTE;
    }

    // Counter：保存一个用户的限流状态。
    // windowStartedAt 是窗口开始时间；used 是当前窗口已经使用的请求次数。
    private record Counter(long windowStartedAt, AtomicInteger used) {
    }
}
