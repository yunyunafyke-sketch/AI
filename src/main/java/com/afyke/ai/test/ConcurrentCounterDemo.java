package com.afyke.ai.test;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理“用户 ID → 该用户的 Counter”。
 */
public class ConcurrentCounterDemo {

    // 所有请求线程共享的一张表。
    // final 表示 counters 只能在构造方法中赋值一次，之后不能换成另一张 Map；
    // 但仍然可以向这张 Map 放入或读取“用户 ID → Counter”数据。
    private final ConcurrentHashMap<String, Counter> counters;

    /**
     * new ConcurrentCounterDemo() 时会自动调用这里。
     * 它只创建一张空表，不会创建任何用户的 Counter，也不会给任何用户加次数。
     */
    public ConcurrentCounterDemo() {
        this.counters = new ConcurrentHashMap<>();
    }

    /**
     * 记录一次指定用户的请求，并返回该用户加完后的累计次数。
     */
    public int recordRequest(String userId) {
        // computeIfAbsent 的意思是“如果不存在，就创建”。
        // 先找 userId 对应的 Counter：找到了，直接返回已有 Counter；
        // 找不到时，才执行 id -> new Counter() 创建一个、放入 counters，再返回这个新 Counter。
        // id 是 Lambda 表达式接到的 userId；本例不需要使用它。
        Counter counter = counters.computeIfAbsent(userId, id -> new Counter());

        // 只给这个用户对应的 Counter 加一。
        return counter.increment();
    }

    /**
     * 查询一个用户当前的次数；这里不加一，只读取。
     */
    public int getRequestCount(String userId) {
        Counter counter = counters.get(userId);
        return counter == null ? 0 : counter.get();
    }

    public static void main(String[] args) throws InterruptedException {
        // 创建“计数管理器”：此刻只得到一张空的 counters 表。
        ConcurrentCounterDemo counterService = new ConcurrentCounterDemo();

        int threadCount = 10;
        int requestsPerThread = 1_000;
        // 用数组保存创建出的 10 个线程，后面要逐个等待它们结束。
        Thread[] threads = new Thread[threadCount];

        // 创建 10 个工作线程：每个线程处理一个不同用户的 1,000 次请求。
        for (int i = 0; i < threadCount; i++) {
            // 每次循环都得到不同的用户 ID，例如 user-1001、user-1002。
            String userId = "user-" + (1001 + i);

            // 新线程启动后，会反复调用同一个 counterService 的 recordRequest 方法。
            threads[i] = new Thread(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    counterService.recordRequest(userId);
                }
            });

            // start() 才会让这个新线程开始执行上面的 Lambda 代码。
            threads[i].start();
        }

        // join() 表示 main 线程在这里等待：某个工作线程没结束，就先不继续。
        // 等 10 个线程都结束后，才可以安全地打印最终结果。
        for (Thread thread : threads) {
            thread.join();
        }

        // 每个用户都应当有 1,000 次请求。
        for (int i = 0; i < threadCount; i++) {
            String userId = "user-" + (1001 + i);
            System.out.println(userId + "：" + counterService.getRequestCount(userId));
        }
    }
}