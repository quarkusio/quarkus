package io.quarkus.undertow.test.devmode;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.quarkus.runtime.BlockingOperationControl;

@WebServlet(urlPatterns = "/new")
public class NewServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!BlockingOperationControl.isBlockingAllowed()) {
            //https://github.com/quarkusio/quarkus/issues/7782
            throw new RuntimeException("Must not be on IO thread");
        }
        resp.getWriter().write("A new Servlet");
    }
}
