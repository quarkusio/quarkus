package io.quarkus.it.corestuff;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.smallrye.config.Config;

@WebServlet(name = "CustomConfigTestingEndpoint", urlPatterns = "/core/config-test")
public class CustomConfigTesting extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final Optional<String> strVal = Config.get().getOptionalValue("test.custom.config", String.class);
        resp.getWriter().write(strVal.isPresent() && strVal.get().equals("custom") ? "OK" : "KO");
    }
}
