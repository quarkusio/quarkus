package io.quarkus.it.rest.client.multipart;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;

@Path("/echo")
@RegisterRestClient(configKey = "multipart-chunks-client")
public interface MultipartChunksClient {

    @POST
    @Path("/chunked")
    String sendChunkedPayload(@RestForm byte[] data);
}
