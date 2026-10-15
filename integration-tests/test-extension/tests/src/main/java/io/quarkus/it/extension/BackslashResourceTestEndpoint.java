package io.quarkus.it.extension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "BackslashResourceTestEndpoint", urlPatterns = "/core/glob-resource")
public class BackslashResourceTestEndpoint extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(req.getParameter("path"))) {
            if (is == null) {
                resp.getWriter().write("Resource not found");
            } else {
                resp.getWriter().write(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }
}
