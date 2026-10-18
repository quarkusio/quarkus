package io.quarkus.undertow.test;

import java.io.IOException;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/roles-bean")
public class RolesAllowedBeanServlet extends HttpServlet {

    @Inject
    RolesAllowedBean rolesAllowedBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(rolesAllowedBean.message());
    }

    @ApplicationScoped
    @RolesAllowed("admin")
    public static class RolesAllowedBean {

        public String message() {
            return "hello";
        }
    }
}
