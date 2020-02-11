package io.quarkus.undertow.test;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

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
