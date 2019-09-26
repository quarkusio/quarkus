package io.quarkus.elytron.security.jdbc;

import java.io.IOException;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Basic secured servlet test target
 */
@ServletSecurity(@HttpConstraint(rolesAllowed = { "user" }))
@WebServlet(name = "SingleRoleSecuredServlet", urlPatterns = "/servlet-secured", initParams = {
        @WebInitParam(name = "message", value = "A secured message") })
public class SingleRoleSecuredServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write(getInitParameter("message"));
    }
}
