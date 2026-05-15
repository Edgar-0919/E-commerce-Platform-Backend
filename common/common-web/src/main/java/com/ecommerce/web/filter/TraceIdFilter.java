package com.ecommerce.web.filter;

import com.ecommerce.core.constant.GlobalConstants;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // 优先使用上游传入的 TraceId（Gateway 已生成），不存在则本地生成
        String traceId = httpRequest.getHeader(GlobalConstants.TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        // 放入 MDC，logback 可通过 %X{traceId} 输出到日志
        MDC.put("traceId", traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear(); // 防止线程复用时的上下文污染
        }
    }
}
