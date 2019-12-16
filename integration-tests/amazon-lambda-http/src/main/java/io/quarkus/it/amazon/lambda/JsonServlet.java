package io.quarkus.it.amazon.lambda;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebServlet(name = "ServletJson", urlPatterns = "/servlet/json")
public class JsonServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        HashMap<Object, Object> map = new HashMap<>();
        map.put("hello", "world");

        resp.setStatus(200);
        resp.addHeader("Content-Type", MediaType.APPLICATION_JSON);
        resp.getWriter().write(mapper.writeValueAsString(map));
    }
}
