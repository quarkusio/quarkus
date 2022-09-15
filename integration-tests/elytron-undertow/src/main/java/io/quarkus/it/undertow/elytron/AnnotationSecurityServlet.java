package io.quarkus.it.undertow.elytron;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.HttpMethodConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
