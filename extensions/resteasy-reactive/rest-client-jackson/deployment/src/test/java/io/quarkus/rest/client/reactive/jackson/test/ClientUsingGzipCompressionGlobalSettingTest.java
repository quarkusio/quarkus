package io.quarkus.rest.client.reactive.jackson.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.Compressed;

public class ClientUsingGzipCompressionGlobalSettingTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyResource.class, Message.class, MyClient.class))
            .overrideConfigKey("quarkus.http.enable-compression", "true")
            .overrideRuntimeConfigKey("quarkus.rest-client.my-client.url", "http://localhost:${quarkus.http.test-port:8081}");

    @RestClient
    MyClient client;

    @Test
    public void testClientSupportCompressedMessagesWithGzip() {
        Message actual = client.receiveCompressed();
        Assertions.assertEquals(1, actual.id);
    }

    @Test
    public void testClientStillWorksWhenMessageIsUncompressed() {
        Message actual = client.receiveUncompressed();
        Assertions.assertEquals(1, actual.id);
    }

    @Path("/client")
    @RegisterRestClient(configKey = "my-client")
    public interface MyClient {

        // This header is used to reproduce the issue: it will force the server to produce the payload with gzip compression
        @ClientHeaderParam(name = "Accept-Encoding", value = "gzip")
        @GET
        @Path("/message")
        Message receiveCompressed();

        @GET
        @Path("/message")
        Message receiveUncompressed();

    }

    @Path("/client")
    public static class MyResource {

        @Compressed
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("/message")
        public String receive() {
            return "{\"id\": 1}";
        }

    }

    public static class Message {
        public int id;
    }
}
