package io.quarkus.it.websocket;

import javax.enterprise.inject.spi.DeploymentException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

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

        } catch (DeploymentException | javax.websocket.DeploymentException e) {
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
