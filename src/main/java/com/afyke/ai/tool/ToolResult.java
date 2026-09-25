package com.afyke.ai.tool;

import java.util.List;

/**
 * Tool 返回给模型的统一结果。
 *
 * @param success 是否成功
 * @param code    稳定结果码，成功时为 OK
 * @param message 给模型阅读的简短说明
 * @param data    成功时的业务数据
 * @param errors  参数错误明细；成功时为空列表
 */
public record ToolResult<T>(
        boolean success,
        String code,
        String message,
        T data,
        List<String> errors
) {

    /**
     * success：构造成功结果，避免每个 Tool 重复填写固定字段。
     */
    public static <T> ToolResult<T> success(String message, T data) {
        return new ToolResult<>(true, "OK", message, data, List.of());
    }

    /**
     * failure：构造失败结果，只返回受控错误，不暴露异常堆栈。
     */
    public static <T> ToolResult<T> failure(
            String code,
            String message,
            List<String> errors) {
        return new ToolResult<>(false, code, message, null, List.copyOf(errors));
    }
}