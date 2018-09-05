package org.jboss.shamrock.example.rest;

import java.net.URL;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/client")
public class ClientResource {

    @Inject
    @RestClient
    private RestInterface restInterface;

    @GET
    @Path("/manual")
    public String manual() throws Exception {

        RestInterface iface = RestClientBuilder.newBuilder()
                .baseUrl(new URL("http", "localhost", 8080, "/rest"))
                .build(RestInterface.class);
        return iface.get();
    }

    @GET
    @Path("/cdi")
    public String cdi() throws Exception {
        return restInterface.get();
    }

}
