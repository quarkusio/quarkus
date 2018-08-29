package org.jboss.shamrock.example;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet(name = "InjectionServlet", urlPatterns = "/injection")
public class InjectionServlet extends HttpServlet {

    @Inject
    MessageBean messageBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write(messageBean.getMessage());
    }
}
