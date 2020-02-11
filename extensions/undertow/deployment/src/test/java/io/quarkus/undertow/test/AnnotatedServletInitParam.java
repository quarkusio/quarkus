package io.quarkus.undertow.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
