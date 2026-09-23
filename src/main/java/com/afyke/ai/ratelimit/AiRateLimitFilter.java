package com.afyke.ai.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 限制同一个用户调用 AI 流式接口的频率，防止有人短时间内反复请求，消耗大量模型额度。
 */

/**
 * 请求 /api/chat/stream
 *         ↓
 * AiRateLimitFilter 拦截请求
 *         ↓
 * 取出 X-User-Id
 *         ↓
 * FixedWindowRateLimiter 检查最近60秒用了几次
 *         ↓
 * 前5次：放行到 ChatController
 * 第6次：返回 HTTP 429，不再调用 AI
 */

// Component：把过滤器交给 Spring 管理并自动注册，使请求进入 Controller 前先经过这里。
@Component
// OncePerRequestFilter：保证当前过滤逻辑在一次请求的单个请求线程中只执行一次。
public class AiRateLimitFilter extends OncePerRequestFilter {

    // 真正负责“按用户计数并判断是否超限”的固定窗口限流器。
    private final FixedWindowRateLimiter rateLimiter;

    // Spring 自动注入同一个 FixedWindowRateLimiter Bean，Filter 不需要手动 new。
    public AiRateLimitFilter(FixedWindowRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * 它负责拦截请求、询问限流器是否放行。
     * @param request
     * @return
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // shouldNotFilter：返回 true 表示“跳过这个过滤器”，返回 false 才会执行下面的限流检查。
        // 这里只有 /api/chat/stream 返回 false，因此只限制 AI 流式接口，不影响其他接口。
        return !"/api/chat/stream".equals(request.getRequestURI());
    }

    @Override
    // doFilterInternal：处理进入 Controller 前的请求，并决定“拒绝请求”还是“继续放行”。
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // getHeader：读取请求头中的用户标识。例如 X-User-Id: user-1001。
        // 这里只是学习写法；生产环境必须从认证后的登录态读取可信用户 ID，不能相信客户端随意填写的请求头。
        String userId = request.getHeader("X-User-Id");

        // 没有用户 ID 时统一使用 anonymous；这意味着所有匿名请求会共同使用每分钟 5 次的额度。
        String limitKey = (userId == null || userId.isBlank()) ? "anonymous" : userId;

        // allow：让限流器记录本次请求并判断是否放行；false 表示当前用户已经超过限制。
        if (!rateLimiter.allow(limitKey)) {
            // HTTP 429 Too Many Requests：告诉客户端请求过于频繁。
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            // 返回 JSON，并明确使用 UTF-8，避免中文提示乱码。
            response.setContentType("application/json;charset=UTF-8");
            // 把错误信息直接写入响应体；这里不会再进入 ChatController，也不会调用 AI。
            response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"请求过于频繁，请 1 分钟后再试\"}");
            // return：立即结束当前过滤流程，防止下面的 doFilter 继续放行请求。
            return;
        }

        // doFilter：限流检查通过，把请求和响应继续交给后面的过滤器，最终进入 ChatController。
        filterChain.doFilter(request, response);
    }
}
