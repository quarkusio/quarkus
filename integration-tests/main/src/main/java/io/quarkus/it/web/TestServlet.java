package io.quarkus.it.web;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@WebServlet(name = "MyServlet", urlPatterns = "/testservlet", initParams = {
        @WebInitParam(name = "message", value = "A message") })
public class TestServlet extends HttpServlet {

    @Inject
    @ConfigProperty(name = "web-message")
    String configMessage;

    @Inject
    HttpServletResponse injectedResponse;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        injectedResponse.getWriter().write(configMessage);
    }
}
