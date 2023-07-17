package io.quarkus.it.websocket;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

import io.quarkus.runtime.annotations.RegisterForReflection;

@WebListener
public class AddWebSocketHandler implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            ((ServerContainer) sce.getServletContext().getAttribute(ServerContainer.class.getName()))
                    .addEndpoint(ServerEndpointConfig.Builder.create(WebSocketEndpoint.class, "/added-dynamic").build());

        } catch (DeploymentException | jakarta.websocket.DeploymentException e) {
            throw new RuntimeException(e);
        }
    }

    @RegisterForReflection
    public static class WebSocketEndpoint extends Endpoint {

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            session.getAsyncRemote().sendText("DYNAMIC");
        }

    }
}
