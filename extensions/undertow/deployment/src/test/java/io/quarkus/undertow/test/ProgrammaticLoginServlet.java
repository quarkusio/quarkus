package io.quarkus.undertow.test;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Logs in and out using {@link HttpServletRequest#login(String, String)} and {@link HttpServletRequest#logout()}, and
 * reports the current remote user on a path that is secured by HTTP permissions.
 */
@WebServlet(urlPatterns = { "/programmatic/login", "/programmatic/logout", "/secured/user" })
public class ProgrammaticLoginServlet extends HttpServlet {

    public static final String USER = "user";
    public static final String PASSWORD = "password";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (req.getServletPath()) {
            case "/programmatic/login":
                try {
                    req.login(req.getParameter(USER), req.getParameter(PASSWORD));
                } catch (ServletException ex) {
                    resp.sendError(401);
                    return;
                }
                break;
            case "/programmatic/logout":
                req.logout();
                break;
            case "/secured/user":
                break;
            default:
                resp.sendError(404);
                return;
        }
        resp.getWriter().write(req.getRemoteUser() == null ? "" : req.getRemoteUser());
    }
}
