package io.quarkus.resteasy.runtime;

import java.io.IOException;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;

public class ResteasyServlet extends HttpServlet30Dispatcher {
    @Override
    public void service(String httpMethod, HttpServletRequest request, HttpServletResponse response) throws IOException {
        CurrentVertxRequest currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
        ContextUtil.pushContext(currentVertxRequest.getCurrent());

        super.service(httpMethod, request, response);
    }
}
