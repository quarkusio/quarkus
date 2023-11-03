package io.quarkus.undertow.test;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class WebXmlInitParamsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().println(getServletConfig().getInitParameter("ThisIsParameterName1"));
        resp.getWriter().println(getServletConfig().getInitParameter("ThisIsParameterName2"));
        resp.getWriter().println(getServletContext().getInitParameter("MyContextParamName1"));
        resp.getWriter().println(getServletContext().getInitParameter("MyContextParamName2"));
    }
}
