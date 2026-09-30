package com.sleekydz86.catalog.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class WorkerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String WORKER_TOKEN_HEADER = "X-Worker-Token";
    private static final String WORKER_PRINCIPAL = "extract-worker";

    private final String workerToken;

    public WorkerTokenAuthenticationFilter(String workerToken) {
        this.workerToken = workerToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/internal/worker/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String presented = resolveToken(request);
        if (!StringUtils.hasText(presented) || !workerToken.equals(presented)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(new WorkerTokenAuthentication(WORKER_PRINCIPAL));
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String workerHeader = request.getHeader(WORKER_TOKEN_HEADER);
        if (StringUtils.hasText(workerHeader)) {
            return workerHeader.trim();
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        return null;
    }
}
