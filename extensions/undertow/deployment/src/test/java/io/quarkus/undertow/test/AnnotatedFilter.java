package io.quarkus.undertow.test;

import java.io.IOException;
import java.util.Enumeration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;

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
