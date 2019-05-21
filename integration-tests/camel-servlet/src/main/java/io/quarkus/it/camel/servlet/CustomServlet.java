package io.quarkus.it.camel.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;

@SuppressWarnings("serial")
@WebServlet
public class CustomServlet extends CamelHttpTransportServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /* Set this header and assert in the test that the request was served by this servlet */
        resp.setHeader("x-servlet-class-name", this.getClass().getName());
        super.service(req, resp);
    }

}
