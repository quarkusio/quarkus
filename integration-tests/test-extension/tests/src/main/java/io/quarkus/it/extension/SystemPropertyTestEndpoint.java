package io.quarkus.it.extension;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "SystemPropertyTestEndpoint", urlPatterns = "/core/sysprop")
public class SystemPropertyTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write(System.getProperty("quarkus.dymmy", "unset"));
    }

}
