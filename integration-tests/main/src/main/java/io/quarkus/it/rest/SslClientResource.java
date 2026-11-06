package io.quarkus.it.rest;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Creates the {@link SslRestInterface} client without calling it, and returns the messages of the exception chain
 * thrown when doing so: in native mode, quarkus.ssl.native=false prevents using its https URL.
 */
@Path("/ssl")
public class SslClientResource {

    @Inject
    @RestClient
    Instance<SslRestInterface> sslRestInterface;

    @GET
    public String createClient() {
        try {
            sslRestInterface.get();
            return "created";
        } catch (RuntimeException e) {
            StringBuilder messages = new StringBuilder();
            for (Throwable t = e; t != null; t = t.getCause()) {
                messages.append(t.getMessage()).append('\n');
            }
            return messages.toString();
        }
    }

}
