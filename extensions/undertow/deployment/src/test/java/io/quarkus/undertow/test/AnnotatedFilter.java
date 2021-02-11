package io.quarkus.undertow.test;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;

@WebFilter(urlPatterns = "/*")
public class AnnotatedFilter extends HttpFilter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        Enumeration<String> attributeNames = request.getServletContext().getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            response.getWriter().println(attributeNames.nextElement());
        }
        response.getWriter().println("annotated filter");
        chain.doFilter(request, response);
    }
}
