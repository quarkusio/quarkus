package io.quarkus.it.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/ws-client-coding-test")
public class ClientCodingResource {

    @GET
    public String codeBothWays(@QueryParam("echoServerUri") String echoServerUri) throws IOException, DeploymentException {
        ContainerProvider.getWebSocketContainer()
                .connectToServer(CodingClient.class, URI.create(echoServerUri));

        try {
            Dto response = CodingClient.messageQueue.poll(20, TimeUnit.SECONDS);
            return response.getContent();
        } catch (InterruptedException e) {
            CodingClient.close();
            return "Failed to get response in time";
        }
    }
}
