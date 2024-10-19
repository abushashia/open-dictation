package com.sitedictation.username;

import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;

@Component
class UserNameFilter implements Filter {

    private final UserNameHelper userNameHelper;

    UserNameFilter(UserNameHelper userNameHelper) {
        this.userNameHelper = userNameHelper;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterchain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        Principal principal = httpServletRequest.getUserPrincipal();
        httpServletRequest.setAttribute("userName", userNameHelper.getUserName(principal));
        filterchain.doFilter(httpServletRequest, servletResponse);
    }
}
