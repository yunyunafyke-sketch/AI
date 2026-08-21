# AI

这是一个基于 Spring Boot 和 Spring AI `ChatClient` 的大模型调用示例项目。

项目当前通过 OpenAI 类型的 Starter 创建 `ChatModel`，再由 `ChatClient` 发送用户问题并返回模型回答。

## 1. 项目基本信息

| 项目 | 当前配置 |
| --- | --- |
| Spring Boot | `4.1.1` |
| Java | `21` |
| Spring AI | `2.0.0` |
| Maven | `3.9.x` |
| 启动类 | `com.afyke.ai.AiApplication` |
| Controller | `com.afyke.ai.controller.AiController` |
| 接口 | `GET /ai?question=问题内容` |

## 2. 运行前准备

### 2.1 Java 21

当前本机 Java 21 路径：

```text
/Volumes/data/develop/JDK/jdk21/Contents/Home
```

Spring Boot 4.1.1 项目使用 Java 21 编译和运行。公司项目使用 Java 8 时，不要修改公司项目的配置；只在本项目中设置 Java 21。

### 2.2 Maven 3.9

当前本机 Maven 3.9.16 路径：

```text
/Volumes/data/develop/apache-maven-3.9.16
```

项目当前没有 `mvnw` 文件，因此不能执行 `./mvnw spring-boot:run`。可以使用 IntelliJ IDEA 启动，或者使用本机安装的 Maven 3.9。

### 2.3 模型服务和 API Key

项目需要一个 OpenAI 或 OpenAI 兼容模型服务，并需要对应的 API Key。

当前配置文件是：

```text
src/main/resources/application.properties
```

当前配置内容：

```properties
spring.application.name=AI
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.model=deepseek-v4-flash
```

其中：

- `OPENAI_API_KEY`：运行时从环境变量读取的 API Key。
- `spring.ai.openai.chat.model`：实际请求的模型名称。
- `deepseek-v4-flash`：当前项目配置的模型名，必须确认模型服务商确实支持这个名称。
- 如果使用的不是 OpenAI 官方接口，还需要配置服务商提供的 `spring.ai.openai.base-url`。

不要把真实 API Key 直接提交到 Git。

## 3. 项目依赖

当前 `pom.xml` 中主要包含：

- Spring WebFlux
- Spring WebMVC
- Spring AI OpenAI Starter
- Spring AI Ollama Starter
- Spring Boot DevTools
- Lombok
- WebFlux 和 WebMVC 测试依赖

当前项目不使用本地 Ollama。若启动时出现多个 `ChatModel` 或 `ChatClient.Builder` 注入冲突，可以删除下面这项依赖：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>
```

保留 OpenAI Starter：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

## 4. IntelliJ IDEA 启动方式

这是本项目推荐的启动方式。

### 4.1 设置当前项目的 Java 版本

打开：

```text
File → Project Structure → Project
```

设置：

```text
Project SDK: Java 21
Language level: 21
```

如果没有 Java 21，添加下面的 JDK 路径：

```text
/Volumes/data/develop/JDK/jdk21/Contents/Home
```

### 4.2 设置当前项目的 Maven

打开：

```text
IntelliJ IDEA → Settings
→ Build, Execution, Deployment
→ Build Tools → Maven
```

设置：

```text
Maven home path:
/Volumes/data/develop/apache-maven-3.9.16
```

进入：

```text
Build Tools → Maven → Runner
```

将 `JRE` 设置为：

```text
Project SDK 21
```

在 `Maven → Importing` 中，也将导入 JDK 设置为 `Project SDK 21`。

### 4.3 配置 API Key

打开：

```text
Run → Edit Configurations → AiApplication
```

在 `Environment variables` 中添加：

```text
OPENAI_API_KEY=你的真实APIKey
```

不要把真实 API Key 写进 `application.properties` 并提交到 Git。

### 4.4 启动项目

1. 打开 `src/main/java/com/afyke/ai/AiApplication.java`。
2. 确认启动配置使用 Java 21。
3. 点击类左侧或顶部的绿色运行按钮。
4. 选择 `Run 'AiApplication'`。

启动类会执行：

```java
SpringApplication.run(AiApplication.class, args);
```

启动成功后，默认监听：

```text
http://localhost:8080
```

## 5. 终端启动方式

终端启动不是必须方式，只是 IntelliJ IDEA 启动之外的另一种选择。

在终端中进入项目目录：

```bash
cd /Volumes/data/ideaWorkSpace/AI
```

本项目没有 `mvnw`，所以使用 Maven 3.9.16 的完整路径，并临时指定 Java 21：

```bash
JAVA_HOME="/Volumes/data/develop/JDK/jdk21/Contents/Home" \
PATH="/Volumes/data/develop/apache-maven-3.9.16/bin:$PATH" \
OPENAI_API_KEY="你的真实APIKey" \
"/Volumes/data/develop/apache-maven-3.9.16/bin/mvn" spring-boot:run
```

这只对当前命令生效，不会修改公司项目使用的 Java 8。

启动前可以先验证 Java 和 Maven：

```bash
JAVA_HOME="/Volumes/data/develop/JDK/jdk21/Contents/Home" \
PATH="/Volumes/data/develop/apache-maven-3.9.16/bin:$PATH" \
"/Volumes/data/develop/apache-maven-3.9.16/bin/mvn" -version
```

应该看到 Java 21 和 Maven 3.9.16。

## 6. 调用接口

启动项目后，接口地址为：

```text
GET http://localhost:8080/ai?question=你的问题
```

### 6.1 浏览器调用

在浏览器中打开：

```text
http://localhost:8080/ai?question=请用一句话解释Spring IOC
```

### 6.2 curl 调用

```bash
curl --get "http://localhost:8080/ai" \
  --data-urlencode "question=请用一句话解释 Spring IOC"
```

### 6.3 Java 代码执行流程

`AiController` 中的核心代码是：

```java
return chatClient
        .prompt()
        .user(question)
        .call()
        .content();
```

执行过程：

1. 接收请求参数 `question`。
2. `ChatClient` 组装用户消息。
3. 底层 `OpenAiChatModel` 根据配置调用模型。
4. 模型生成回答。
5. `content()` 返回文本内容。

## 7. 常见启动问题

### 7.1 Java 版本错误

如果日志提示 Java 版本过低，检查：

- IntelliJ 项目 SDK 是否为 Java 21。
- Maven Runner 的 JRE 是否为 Project SDK 21。
- 启动配置的 JRE 是否为 Java 21。
- 终端启动时是否设置了 `JAVA_HOME`。

### 7.2 API Key 缺失

如果出现 API Key 为空或认证失败，检查：

- Run Configuration 中是否添加了 `OPENAI_API_KEY`。
- 终端启动命令中是否传入了 `OPENAI_API_KEY`。
- API Key 是否有效。

### 7.3 模型不存在或地址错误

如果出现模型不存在、404 或请求地址错误，检查：

- `spring.ai.openai.chat.model` 是否为服务商支持的模型名。
- 使用 OpenAI 兼容接口时，是否配置了正确的 `spring.ai.openai.base-url`。
- API Key 是否属于对应的模型服务商。

### 7.4 端口被占用

如果 `8080` 端口已经被占用，可以在 `application.properties` 中增加：

```properties
server.port=8081
```

之后访问：

```text
http://localhost:8081/ai?question=你好
```

### 7.5 多个 ChatModel 冲突

如果出现 `ChatClient.Builder` 或 `ChatModel` Bean 注入冲突，检查 `pom.xml` 是否同时启用了多个模型 Starter。

本项目不使用本地 Ollama，可以优先移除：

```xml
<artifactId>spring-ai-starter-model-ollama</artifactId>
```

## 8. 停止项目

- IntelliJ IDEA：点击运行窗口中的红色停止按钮。
- 终端：按 `Ctrl + C`。

## 9. 项目结构

```text
AI/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/afyke/ai/
    │   │   ├── AiApplication.java
    │   │   └── controller/AiController.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/afyke/ai/AiApplicationTests.java
```
