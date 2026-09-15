package com.afyke.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 告诉 Spring：这个类负责声明配置 Bean，应用启动时会读取它。
@Configuration
public class AiExecutorConfig {

    // 注册名为 aiExecutor 的线程池 Bean；Spring 关闭时自动调用 shutdown()，不再接收新任务。
    @Bean(destroyMethod = "shutdown")
    ExecutorService aiExecutor() {
        // ChatClient.call() 是阻塞调用。这里最多同时运行 8 个模型调用任务。
        // 超过 8 个的任务会先在该工厂默认的等待队列中排队；本篇用于学习，生产应改为有界队列。
        return Executors.newFixedThreadPool(8);
    }
}