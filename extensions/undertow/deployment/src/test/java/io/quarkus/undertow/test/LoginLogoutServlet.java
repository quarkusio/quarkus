package io.quarkus.undertow.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.quarkus.security.AuthenticationFailedException;

@WebServlet(urlPatterns = { "/login", "/logout" })
public class LoginLogoutServlet extends HttpServlet {

    public static final String USER = "user";
    public static final String PASSWORD = "password";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/login".equals(req.getServletPath())) {
            try {
                req.login(req.getParameter(USER), req.getParameter(PASSWORD));
            } catch (ServletException ex) {
                if (ex.getRootCause() instanceof AuthenticationFailedException) {
                    resp.sendError(401);
                } else {
                    resp.sendError(500);
                }
            }
        } else if ("/logout".equals(req.getServletPath())) {
            req.getSession().invalidate();
        } else {
            resp.sendError(404);
        }
    }
}
