package io.quarkus.undertow.test.compress;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = SimpleServlet.SERVLET_ENDPOINT)
public class SimpleServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_ENDPOINT = "/simple";

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        // this one must be listed in the quarkus.http.compress-media-types
        resp.setHeader("Content-type", "text/plain");
        resp.getWriter().write("ok");
    }
}
