package io.quarkus.rest.client.reactive;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.vertx.http.HttpServer;

@ApplicationScoped
public class TestBean {
    @Inject
    HttpServer httpServer;
    @RestClient
    HelloClient2 client2;
    @RestClient
    HelloNonSimpleClient clientNonSimple;

    String helloViaInjectedClient(String name) {
        return client2.echo(name);
    }

    String helloViaInjectedClientIgnoreParams(String name) {
        return client2.echoIgnoreParams(name, "whatever", "whatever2");
    }

    String helloViaBuiltClient(String name) {
        HelloClient helloClient = RestClientBuilder.newBuilder()
                .baseUri(httpServer.getLocalBaseUri())
                .build(HelloClient.class);
        return helloClient.echo(name);
    }

    String bug18977() {
        return client2.bug18977();
    }

    byte[] helloNonSimpleSyncBytes() {
        return clientNonSimple.echoSyncBytes(new byte[] { 1, 2, 3 });
    }

    Integer[] helloNonSimpleSyncInts() {
        return clientNonSimple.echoSyncInts(new Integer[] { 1, 2, 3 });
    }

    Map<String, String> helloQueryParamsToMap() {
        return clientNonSimple.echoQueryAsMap("1", "2", "3", "4", "5", "6");
    }
}
