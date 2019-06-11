/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
