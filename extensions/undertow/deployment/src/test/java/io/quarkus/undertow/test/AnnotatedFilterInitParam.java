package io.quarkus.undertow.test;

import static io.quarkus.undertow.test.AnnotatedServletInitParam.SERVLET_ENDPOINT;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpFilter;

@WebFilter(urlPatterns = SERVLET_ENDPOINT, description = "Haha Filter", initParams = {
        @WebInitParam(name = "AnnotatedInitFilterParamName", value = "AnnotatedInitFilterParamValue", description = "Described filter init param")
})
public class AnnotatedFilterInitParam extends HttpFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        response.getWriter().println("invoked-before-chain");
        response.getWriter().println(getInitParameter("AnnotatedInitFilterParamName"));
        chain.doFilter(request, response);
        response.getWriter().println("invoked-after-chain");
    }
}
