package io.quarkus.it.corestuff;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.ConfigProvider;

@WebServlet(name = "CustomConfigTestingEndpoint", urlPatterns = "/core/config-test")
public class CustomConfigTesting extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final Optional<String> strVal = ConfigProvider.getConfig().getOptionalValue("test.custom.config", String.class);
        resp.getWriter().write(strVal.isPresent() && strVal.get().equals("custom") ? "OK" : "KO");
    }
}
