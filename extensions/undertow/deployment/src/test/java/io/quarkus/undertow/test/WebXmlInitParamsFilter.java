package io.quarkus.undertow.test;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class WebXmlInitParamsFilter implements Filter {
    private FilterConfig filterConfig;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
            FilterChain chain) throws IOException, ServletException {
        resp.getWriter().println("invoked-before-chain");
        resp.getWriter().println(filterConfig.getInitParameter("MyFilterParamName1"));
        resp.getWriter().println(filterConfig.getInitParameter("MyFilterParamName2"));
        chain.doFilter(req, resp);
        resp.getWriter().println("invoked-after-chain");
    }
}
