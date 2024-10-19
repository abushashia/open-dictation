package com.sitedictation;

import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
class CurrentTimeMillisFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterchain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        long currentTimeMillis;
        try {
            currentTimeMillis = Long.parseLong(httpServletRequest.getParameter("currentTimeMillis"));
        } catch (NumberFormatException e) {
            currentTimeMillis = System.currentTimeMillis();
        }
        httpServletRequest.setAttribute("currentTimeMillis", currentTimeMillis);
        filterchain.doFilter(httpServletRequest, servletResponse);
    }

    @Override
    public void init(FilterConfig filterconfig) {
        // empty block
    }

    @Override
    public void destroy() {
        // empty block
    }
}
