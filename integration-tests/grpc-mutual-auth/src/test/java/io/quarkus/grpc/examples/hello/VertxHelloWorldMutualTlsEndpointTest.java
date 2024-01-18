package io.quarkus.grpc.examples.hello;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
class VertxHelloWorldMutualTlsEndpointTest extends VertxHelloWorldMutualTlsEndpointTestBase {

    @Inject
    Vertx vertx;

    @Override
    Vertx vertx() {
        return vertx;
    }

    @Test
    public void testRolesHelloWorldServiceUsingBlockingStub() throws Exception {
        Vertx vertx = vertx();
        WebClient client = null;
        try {
            client = create(vertx);
            HttpRequest<Buffer> request = client.get(8444, "localhost", "/hello/blocking-user/neo");
            Future<HttpResponse<Buffer>> fr = request.send();
            String response = fr.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS).bodyAsString();
            assertThat(response).isEqualTo("Hello neo from CN=testclient,O=Default Company Ltd,L=Default City,C=XX");

            request = client.get(8444, "localhost", "/hello/blocking-admin/neo");
            fr = request.send();
            assertThat(fr.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS).bodyAsString())
                    .contains("io.quarkus.security.ForbiddenException");
        } finally {
            if (client != null) {
                client.close();
            }
            close(vertx);
        }
    }

    @Test
    public void testRolesHelloWorldServiceUsingMutinyStub() throws Exception {
        Vertx vertx = vertx();
        WebClient client = null;
        try {
            client = create(vertx);
            HttpRequest<Buffer> request = client.get(8444, "localhost", "/hello/mutiny-user/neo-mutiny");
            Future<HttpResponse<Buffer>> fr = request.send();
            String response = fr.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS).bodyAsString();
            assertThat(response).isEqualTo("Hello neo-mutiny from CN=testclient,O=Default Company Ltd,L=Default City,C=XX");

            request = client.get(8444, "localhost", "/hello/mutiny-admin/neo");
            fr = request.send();
            assertThat(fr.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS).bodyAsString())
                    .contains("io.quarkus.security.ForbiddenException");
        } finally {
            if (client != null) {
                client.close();
            }
            close(vertx);
        }
    }
}
