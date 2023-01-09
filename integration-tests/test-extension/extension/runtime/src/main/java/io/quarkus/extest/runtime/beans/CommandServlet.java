package io.quarkus.extest.runtime.beans;

import java.io.IOException;
import java.security.interfaces.DSAPublicKey;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple command dispatch servlet used for testing the state of the native image
 */
@WebServlet
public class CommandServlet extends HttpServlet {
    @Inject
    DSAPublicKey publicKey;

    @Override
    public void init() throws ServletException {
        super.init();
        log("init, publicKey=" + publicKey);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        log("doGet, " + pathInfo);
        resp.getWriter().write(pathInfo + "-ack");
    }
}
