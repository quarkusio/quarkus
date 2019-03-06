package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.runtime.BeanContainer;

public class AmazonLambdaServlet extends HttpServlet {
    private static final String ALLOWED_METHODS = "POST, OPTIONS";
    private final BeanContainer.Instance<? extends RequestHandler> handler;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Class<?> targetType;

    public AmazonLambdaServlet(final BeanContainer.Instance<? extends RequestHandler> instance, Class<?> targetType) {
        this.handler = instance;
        this.targetType = targetType;
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        addCorsResponseHeaders(resp);
        resp.addHeader("Allow", ALLOWED_METHODS);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        addCorsResponseHeaders(resp);
        String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

        final Object value = mapper.readValue(body, targetType);

        resp.getOutputStream().print(mapper.writeValueAsString(handler.get().handleRequest(value, null)));
    }

    @Override
    public void destroy() {
        handler.close();
    }

    private static void addCorsResponseHeaders(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Credentials", "true");
        response.addHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.addHeader("Access-Control-Max-Age", "86400");
    }
}
