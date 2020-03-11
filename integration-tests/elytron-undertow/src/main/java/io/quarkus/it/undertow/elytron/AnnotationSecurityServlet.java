package io.quarkus.it.undertow.elytron;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "AnnotationSecurityServlet", urlPatterns = "/annotation-secure")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = "managers"), httpMethodConstraints = {
        @HttpMethodConstraint(value = "PUT", emptyRoleSemantic = ServletSecurity.EmptyRoleSemantic.PERMIT),
        @HttpMethodConstraint(value = "DELETE", emptyRoleSemantic = ServletSecurity.EmptyRoleSemantic.DENY),
        @HttpMethodConstraint(value = "POST", rolesAllowed = "interns")
})
public class AnnotationSecurityServlet extends HttpServlet {

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getUserPrincipal().getName() == null) {
            throw new RuntimeException("principal was null");
        }
        resp.setStatus(200);
        resp.addHeader("Content-Type", "text/plain");
        resp.getWriter().write("hello");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getUserPrincipal().getName() == null) {
            throw new RuntimeException("principal was null");
        }
        String name = req.getReader().readLine();
        resp.setStatus(200);
        resp.addHeader("Content-Type", "text/plain");
        resp.getWriter().write("hello " + name);
    }
}
