package io.quarkus.lambda.runtime;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LambdaServlet extends HttpServlet {
    private static final String ALLOWED_METHODS = "POST, OPTIONS";
    private RequestHandler handler;
    private ObjectMapper mapper = new ObjectMapper();
    private Class<?> targetType;

    public LambdaServlet(final Class<? extends RequestHandler> handler) throws ReflectiveOperationException {
        this.handler = handler.newInstance();
        discoverParameterTypes();
    }

    private void discoverParameterTypes() {
        final Method[] methods = handler.getClass().getDeclaredMethods();
        Method method = null;
        for (int i = 0; i < methods.length && method == null; i++) {
            if (methods[i].getName().equals("handleRequest")) {
                final Class<?>[] types = methods[i].getParameterTypes();
                if (types.length == 2 && !types[0].equals(Object.class)) {
                    method = methods[i];
                }
            }
        }
        if (method == null) {
            method = methods[0];
        }
        targetType = method.getParameterTypes()[0];
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

        resp.getOutputStream().print(mapper.writeValueAsString(handler.handleRequest(value, null)));
    }

    private static void addCorsResponseHeaders(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Credentials", "true");
        response.addHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.addHeader("Access-Control-Max-Age", "86400");
    }
}
