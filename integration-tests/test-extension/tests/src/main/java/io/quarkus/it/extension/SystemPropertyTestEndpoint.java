package io.quarkus.it.extension;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "SystemPropertyTestEndpoint", urlPatterns = "/core/sysprop")
public class SystemPropertyTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write(System.getProperty("quarkus.dymmy", "unset"));
    }

}
