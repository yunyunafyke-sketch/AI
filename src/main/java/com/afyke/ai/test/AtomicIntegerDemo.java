package com.afyke.ai.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerDemo {

    // 普通 int：count++ 不是原子操作，并发递增时可能少算。
    private int normalCount = 0;

    // 原子计数器：多个线程共同递增时不会丢失次数。
    private final AtomicInteger requestCount = new AtomicInteger(0);

    /**
     * 普通加一：实际包含“读取、加一、写回”三步。
     */
    public void handleUnsafeRequest() {
        normalCount++;
    }

    /**
     * 原子加一：三步对其他线程来说不可拆开。
     */
    public void handleAtomicRequest() {
        requestCount.incrementAndGet();
    }

    public static void main(String[] args) throws InterruptedException {
        AtomicIntegerDemo demo = new AtomicIntegerDemo();

        int threadCount = 10;
        int requestsPerThread = 10_000;
        // 创建名为 start 的等待器，倒计时初始为 1。
        // 数字未变成 0 前，调用 start.await() 的工作线程会暂停等待。
        CountDownLatch start = new CountDownLatch(1);
        // 创建名为 finished 的等待器，倒计时初始为 10。
        // main 线程会等 10 个工作线程都报告“完成”后，才继续打印结果。
        CountDownLatch finished = new CountDownLatch(threadCount);

        // 循环 10 次，每次创建 1 个工作线程；一共创建 10 个线程。
        for (int i = 0; i < threadCount; i++) {
            // new Thread(...) 表示：创建一个新线程，并让它执行下面花括号中的代码。
            new Thread(() -> {
                try {
                    // 此时 start 的数字还是 1，所以当前工作线程先停在这里等待。
                    // 直到 main 线程执行 start.countDown()，使数字变成 0，才继续向下计数。
                    start.await();
                    for (int j = 0; j < requestsPerThread; j++) {
                        demo.handleUnsafeRequest(); // 可能丢失更新
                        demo.handleAtomicRequest(); // 不会丢失更新
                    }
                } catch (InterruptedException e) {
                    // 示例中恢复中断标记，然后结束当前线程。
                    Thread.currentThread().interrupt();
                } finally {
                    // 无论是否被中断，都通知 main 线程当前任务已结束。
                    finished.countDown();
                }
            }).start();
        }

        // 循环已启动 10 个工作线程。把 start 从 1 减为 0，放行已经在等待的线程。
        // 若某个线程稍后才执行到 start.await()，它发现数字已是 0，也会直接继续。
        start.countDown();
        // main 线程等待所有请求处理完成，再读取最终计数。
        finished.await();

        int expectedCount = threadCount * requestsPerThread;
        System.out.println("预期次数：" + expectedCount);
        System.out.println("普通 count++：" + demo.normalCount);
        System.out.println("AtomicInteger：" + demo.requestCount.get());
    }
}