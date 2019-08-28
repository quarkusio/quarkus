package io.quarkus.it.corestuff;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "CoreReflectionTestEndpoint", urlPatterns = "/core/charsetsupport")
public class CharSetSupport extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write(Charset.isSupported("Cp1252") ? "OK" : "KO");
    }

}
