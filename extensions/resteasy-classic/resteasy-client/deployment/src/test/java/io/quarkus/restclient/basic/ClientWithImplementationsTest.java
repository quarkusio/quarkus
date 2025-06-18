package io.quarkus.restclient.basic;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ClientWithImplementationsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyClient.class, MyImplementation.class))
            .withConfigurationResource("client-with-implementations.properties");

    @RestClient
    MyClient client;

    @Test
    public void testClientBeanHasBeenCreated() {
        Assertions.assertEquals("hello", client.get());
    }

    @Path("/client")
    @RegisterRestClient(configKey = "my-client")
    public interface MyClient {

        @GET
        @Path("/")
        String get();
    }

    public static class MyImplementation implements MyClient {

        @GET
        @Path("/")
        @Override
        public String get() {
            return "hello";
        }
    }
}
