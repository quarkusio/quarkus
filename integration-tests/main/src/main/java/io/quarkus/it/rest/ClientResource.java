package io.quarkus.it.rest;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.arc.Arc;

@Path("/client")
public class ClientResource {

    @Inject
    @RestClient
    RestInterface restInterface;

    @Inject
    @RestClient
    RestClientInterface restClientInterface;

    @Inject
    @RestClient
    RestClientConfigKeyInterface restClientConfigKeyInterface;

    @Inject
    @RestClient
    RestClientBaseUriConfigKeyInterface restClientBaseUriConfigKeyInterface;

    @GET
    @Path("/annotation/configKey")
    public String configKey() {
        return restClientConfigKeyInterface.get();
    }

    @GET
    @Path("/annotation/baseUriConfigKey")
    public String baseUriConfigKey() {
        return restClientBaseUriConfigKeyInterface.get();
    }

    @GET
    @Path("/manual")
    public String manual() throws Exception {
        ProgrammaticRestInterface iface = RestClientBuilder.newBuilder()
                .baseUrl(new URL(ConfigProvider.getConfig().getValue("test.url", String.class)))
                .build(ProgrammaticRestInterface.class);
        return iface.get();
    }

    @GET
    @Path("/cdi")
    public String cdi() {
        return restInterface.get();
    }

    @GET
    @Path("/async/cdi")
    public CompletionStage<String> asyncCdi() {
        return restInterface.asyncGet();
    }

    @GET
    @Path("manual/jackson")
    @Produces("application/json")
    public TestResource.MyData getDataManual() throws Exception {
        ProgrammaticRestInterface iface = RestClientBuilder.newBuilder()
                .baseUrl(new URL(ConfigProvider.getConfig().getValue("test.url", String.class)))
                .build(ProgrammaticRestInterface.class);
        return iface.getData();
    }

    @GET
    @Path("cdi/jackson")
    @Produces("application/json")
    public TestResource.MyData getDataCdi() {
        return restClientInterface.getData();
    }

    @GET
    @Path("async/cdi/jackson")
    @Produces("application/json")
    public CompletionStage<TestResource.MyData> getDataAsync() {
        return restInterface.getDataAsync();
    }

    @GET
    @Path("/manual/complex")
    @Produces("application/json")
    public List<ComponentType> complexManual() throws Exception {
        ProgrammaticRestInterface iface = RestClientBuilder.newBuilder()
                .baseUrl(new URL(ConfigProvider.getConfig().getValue("test.url", String.class)))
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
                .baseUrl(new URL(ConfigProvider.getConfig().getValue("test.url", String.class)))
                .build(ProgrammaticRestInterface.class);
        return client.getAllHeaders();
    }

    @GET
    @Path("/cdi/headers")
    @Produces("application/json")
    public Map<String, String> getAllHeadersCdi(String headerValue) {
        return restInterface.getAllHeaders();
    }

    @GET
    @Path("/cdi/mp-rest-default-scope")
    @Produces("text/plain")
    public String getDefaultScope() {
        return Arc.container().instance(RestInterface.class, RestClient.LITERAL).getBean().getScope().getName();
    }

    @GET
    @Path("/cdi/default-scope-on-interface")
    @Produces("text/plain")
    public String getDefaultInterfaceScope() {
        return Arc.container().instance(RestClientInterface.class, RestClient.LITERAL).getBean().getScope().getName();
    }
}
