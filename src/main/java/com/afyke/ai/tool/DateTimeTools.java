package com.afyke.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeTools {

    // @Tool：把下面的 Java 方法声明为一个可供模型选择的工具。
    // 只有当 DateTimeTools 对象通过 .tools(...) 或 .defaultTools(...) 注册后，
    // Spring AI 才会读取这个注解，并把工具说明发送给模型。
    //
    // description：告诉模型这个工具能做什么、什么时候应该使用。
    // 它会影响模型是否选择该工具，但不会执行 Java 代码，也不负责参数校验。
    @Tool(description = "查询指定时区的当前日期和时间；当用户询问现在几点、今天日期或当前时间时使用")
    public String getCurrentDateTime(
            // @ToolParam：描述 zoneId 参数的含义和格式，
            // Spring AI 会用它生成工具参数的 JSON Schema，帮助模型正确传参。
            //
            // required = false：告诉模型这个参数可以不传。
            // 注意：它只把参数标记为可选，不会自动把参数设置成 Asia/Shanghai。
            @ToolParam(
                    description = "IANA 时区名称，例如 Asia/Shanghai；用户未说明时区时可以不传",
                    required = false)
            String zoneId) {

        // description 只是给模型看的说明，不会自动生成 Java 默认值。
        // 因此参数为空时，仍要由业务代码真正设置默认时区。
        String actualZoneId = (zoneId == null || zoneId.isBlank())
                ? "Asia/Shanghai"
                : zoneId;

        // ZoneId.of：把最终时区字符串转换为 Java 时区对象。
        ZoneId zone = ZoneId.of(actualZoneId);

        // ZonedDateTime.now：由 Java 应用读取真实时间，不让模型猜测。
        ZonedDateTime now = ZonedDateTime.now(zone);

        System.out.println("执行 getCurrentDateTime，zoneId=" + zoneId);
        // format：生成稳定的工具结果，再交给模型组织最终回答。
        return now.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }
}