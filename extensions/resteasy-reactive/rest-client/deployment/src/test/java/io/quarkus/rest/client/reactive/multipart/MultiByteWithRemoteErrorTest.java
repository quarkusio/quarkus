package io.quarkus.rest.client.reactive.multipart;

import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.MediaType;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;

public class MultiByteWithRemoteErrorTest {
    public static final int BYTES_SENT = 5_000_000; // 5 megs

    @Inject
    Vertx vertx;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, Form.class));

    /*
     * try to send 5MB file, server closes the connection after 1MB
     * verify that the client didn't hang and got some exception
     */
    @Test
    @Timeout(10)
    void shouldFailGracefullyOnRemoteError() throws ExecutionException, InterruptedException {
        NetServerOptions options = new NetServerOptions()
                .setHost("localhost");
        AtomicInteger counter = new AtomicInteger();
        NetServer netServer = vertx.createNetServer(options).connectHandler(
                socket -> socket.handler(
                        data -> {
                            if (counter.addAndGet(data.length()) > 1_000_000) {
                                socket.close();
                            }
                        }));

        CompletableFuture<Integer> port = new CompletableFuture<>();
        netServer.listen(server -> port.complete(server.result().actualPort()));

        await().atMost(Duration.ofSeconds(5)).until(port::isDone);

        String uri = String.format("http://localhost:%s", port.get());

        Client client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(uri))
                .build(Client.class);

        Form form = new Form();
        form.file = Multi.createBy().repeating().supplier(() -> (byte) 13).atMost(BYTES_SENT);
        Assertions.assertThatThrownBy(() -> client.post(form)).isInstanceOf(Exception.class);
    }

    public interface Client {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String post(@MultipartForm Form clientForm);
    }

    public static class Form {
        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public Multi<Byte> file;
    }
}
