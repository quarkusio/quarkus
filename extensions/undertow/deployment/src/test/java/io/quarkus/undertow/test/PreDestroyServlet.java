package io.quarkus.undertow.test;

import java.io.IOException;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
