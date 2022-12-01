package io.quarkus.undertow.test;

import java.io.IOException;

import javax.annotation.security.RunAs;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet(urlPatterns = AnnotatedServlet.SERVLET_ENDPOINT)
// we aren't really interested in testing the RunAs feature, instead this is there to reproduce
// a NPE https://github.com/quarkusio/quarkus/issues/5293
@RunAs("dummy")
public class AnnotatedServlet extends HttpServlet {

    public static final String SERVLET_ENDPOINT = "/plainAnnotatedServlet";

    public static final String OK_RESPONSE = "Success";

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(OK_RESPONSE);
    }
}
