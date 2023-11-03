package io.quarkus.it.rest;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.arc.Arc;
import io.smallrye.mutiny.Multi;

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

    @Inject
    @RestClient
    RestClientWithFaultToleranceInterface restClientWithFaultToleranceInterface;

    @Inject
    @ConfigProperty(name = "loopback/mp-rest/url", defaultValue = "http://localhost:8080/loopback")
    Provider<String> loopbackEndpoint;

    @Inject
    Client client;

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
    @Path("encoding")
    public String testEmojis() {
        return restClientInterface.echo(
                "\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00");
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

    @GET
    @Path("/jaxrs-client")
    public Greeting testJaxrsClient() throws ClassNotFoundException {
        Greeting greeting = client.target(loopbackEndpoint.get())
                .request()
                .get(Greeting.class);
        // The LoggingFilter should be programmatically registered in io.quarkus.it.rest.ClientProducer.init()
        if (!client.getConfiguration().isRegistered(Class.forName("io.quarkus.it.rest.LoggingFilter")))
            throw new IllegalStateException("LoggingFilter should be registered on injected Client");
        if (getFilterCount() != 2)
            throw new IllegalStateException("Call count should have been 2 but was " + getFilterCount());
        return greeting;
    }

    private int getFilterCount() {
        try {
            // Must use reflection to check filter call count to ensure that
            // completely decoupled filters are not removed in native mode
            return Class.forName("io.quarkus.it.rest.LoggingFilter")
                    .getDeclaredField("CALL_COUNT")
                    .getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Inject
    @RestClient
    GouvFrGeoApiClient api;

    @Produces(MediaType.SERVER_SENT_EVENTS)
    @GET
    @Path("publisher-client")
    public Multi<String> publisherClient() {
        Set<Commune> communes = api.getCommunes("75007");
        return Multi.createFrom().emitter(emitter -> {
            try {
                communes.forEach((commune) -> {
                    commune.getCodesPostaux().forEach((postalCode) -> {
                        int level = org.jboss.resteasy.core.ResteasyContext.getContextDataLevelCount();
                        api.getCommunes(postalCode).forEach(c -> emitter.emit(c.getCode() + "-" + level));
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
                emitter.fail(e);
            } finally {
                emitter.complete();
            }
        });
    }

    @GET
    @Path("/fault-tolerance")
    public String faultTolerance() {
        return restClientWithFaultToleranceInterface.echo();
    }
}
