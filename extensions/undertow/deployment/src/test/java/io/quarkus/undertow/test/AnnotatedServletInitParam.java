package io.quarkus.undertow.test;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = AnnotatedServletInitParam.SERVLET_ENDPOINT, initParams = {
        @WebInitParam(name = "AnnotatedInitParamName", value = "AnnotatedInitParamValue", description = "This is my init param")
}, description = "Hahahah", loadOnStartup = 1)
public class AnnotatedServletInitParam extends HttpServlet {

    public static final String SERVLET_ENDPOINT = "/annotatedInitParamServlet";

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().println(getInitParameter("AnnotatedInitParamName"));
    }
}
