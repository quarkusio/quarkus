package io.quarkus.undertow.test;

import java.io.IOException;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
