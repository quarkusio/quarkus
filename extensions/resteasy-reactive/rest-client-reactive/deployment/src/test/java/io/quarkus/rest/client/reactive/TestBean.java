package io.quarkus.rest.client.reactive;

import java.net.URL;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class TestBean {

    @ConfigProperty(name = "test-url")
    URL url;

    @RestClient
    HelloClient2 client2;

    String helloViaInjectedClient(String name) {
        return client2.echo(name);
    }

    String helloViaBuiltClient(String name) {
        HelloClient helloClient = RestClientBuilder.newBuilder()
                .baseUrl(url)
                .build(HelloClient.class);
        return helloClient.echo(name);
    }

    String bug18977() {
        return client2.bug18977();
    }
}
