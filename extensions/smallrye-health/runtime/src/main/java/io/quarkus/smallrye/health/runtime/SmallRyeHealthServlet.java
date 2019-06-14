package io.quarkus.smallrye.health.runtime;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;

@SuppressWarnings("serial")
@WebServlet
public class SmallRyeHealthServlet extends HttpServlet {

    @Inject
    SmallRyeHealthReporter reporter;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        SmallRyeHealth health = reporter.getHealth();
        if (health.isDown()) {
            resp.setStatus(503);
        }
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        reporter.reportHealth(resp.getOutputStream(), health);
    }
}
