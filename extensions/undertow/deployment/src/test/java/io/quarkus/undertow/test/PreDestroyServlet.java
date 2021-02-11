package io.quarkus.undertow.test;

import java.io.IOException;

import javax.annotation.PreDestroy;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/destroy", loadOnStartup = 1)
public class PreDestroyServlet extends HttpServlet {

    @PreDestroy
    void stop() {
        Messages.MESSAGES.addFirst("Servlet Destroyed");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write("pre destroy servlet");
    }
}
