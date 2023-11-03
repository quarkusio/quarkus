package io.quarkus.it.extension;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.quarkus.arc.Arc;

@WebServlet(name = "EnvironmentVariableTestEndpoint", urlPatterns = "/core/env")
public class EnvironmentVariableTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        DummyMapping dummyMapping = Arc.container().select(DummyMapping.class).get();
        resp.getWriter().write(dummyMapping.name() + "-" + dummyMapping.age());
    }
}
