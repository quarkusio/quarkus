package io.quarkus.it.web;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
