package io.quarkus.it.rest;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/client")
public class ClientResource {

    @Inject
    @RestClient
    RestInterface restInterface;

    @GET
    @Path("/manual")
    public String manual() throws Exception {
        ProgrammaticRestInterface iface = RestClientBuilder.newBuilder()
                .baseUrl(new URL(System.getProperty("test.url")))
                .build(ProgrammaticRestInterface.class);
        return iface.get();
    }

    @GET
    @Path("/cdi")
    public String cdi() {
        return restInterface.get();
    }

    @GET
    @Path("manual/jackson")
    @Produces("application/json")
    public TestResource.MyData getDataManual() throws Exception {
        ProgrammaticRestInterface iface = RestClientBuilder.newBuilder()
                .baseUrl(new URL(System.getProperty("test.url")))
                .build(ProgrammaticRestInterface.class);
        System.out.println(iface.getData());
        return iface.getData();
    }

    @GET
    @Path("cdi/jackson")
    @Produces("application/json")
    public TestResource.MyData getDataCdi() {
        return restInterface.getData();
    }

    @GET
    @Path("/manual/complex")
    @Produces("application/json")
    public List<ComponentType> complexManual() throws Exception {
        ProgrammaticRestInterface iface = RestClientBuilder.newBuilder()
                .baseUrl(new URL(System.getProperty("test.url")))
                .build(ProgrammaticRestInterface.class);
        System.out.println(iface.complex());
        return iface.complex();
    }

    @GET
    @Path("/cdi/complex")
    @Produces("application/json")
    public List<ComponentType> complexCdi() {
        return restInterface.complex();
    }

    @GET
    @Path("/manual/headers")
    @Produces("application/json")
    public Map<String, String> getAllHeaders(String headerValue) throws Exception {
        ProgrammaticRestInterface client = RestClientBuilder.newBuilder()
                .baseUrl(new URL(System.getProperty("test.url")))
                .build(ProgrammaticRestInterface.class);
        return client.getAllHeaders();
    }

    @GET
    @Path("/cdi/headers")
    @Produces("application/json")
    public Map<String, String> getAllHeadersCdi(String headerValue) {
        return restInterface.getAllHeaders();
    }

}
