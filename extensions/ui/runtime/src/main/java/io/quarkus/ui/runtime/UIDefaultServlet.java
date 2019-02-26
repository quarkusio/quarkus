package io.quarkus.ui.runtime;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.AttachmentKey;

public class UIDefaultServlet implements Servlet {

    public static final AttachmentKey<Boolean> DEFAULT_REQUEST = AttachmentKey.create(Boolean.class);

    @Override
    public void init(ServletConfig config) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        ServletRequestContext.requireCurrent().getExchange().putAttachment(DEFAULT_REQUEST, true);
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
