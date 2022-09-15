package io.quarkus.security.test;

import java.io.IOException;

import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Basic secured servlet test target
 */
@ServletSecurity(@HttpConstraint(rolesAllowed = { "user" }))
@WebServlet(name = "MySecureServlet", urlPatterns = "/secure-test", initParams = {
        @WebInitParam(name = "message", value = "A secured message") })
public class TestSecureServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write(getInitParameter("message"));
    }

}
