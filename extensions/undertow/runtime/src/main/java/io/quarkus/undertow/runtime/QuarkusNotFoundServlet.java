package io.quarkus.undertow.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.quarkus.vertx.http.runtime.devmode.ResourceNotFoundData;
import io.vertx.core.json.Json;

public class QuarkusNotFoundServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResourceNotFoundData resourceNotFoundData = CDI.current().select(ResourceNotFoundData.class).get();
        String accept = req.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            resp.setContentType("application/json");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.getWriter().write(Json.encodePrettily(resourceNotFoundData.getJsonContent()));
        } else {
            //We default to HTML representation
            resp.setContentType("text/html");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.getWriter().write(resourceNotFoundData.getHTMLContent());
        }
    }

}
