package io.quarkus.it.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientResponse;

@QuarkusIntegrationTest
@WithTestResource(UnixDomainSocketTestResource.class)
@DisabledOnOs(OS.WINDOWS)
public class UnixDomainSocketRestClientIT {

    Vertx vertx;
    HttpClient httpClient;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        httpClient = vertx.createHttpClient();
    }

    @Test
    public void testRestClientOverDomainSocket() {
        String address = ConfigProvider.getConfig().getValue("quarkus.http.domain-socket", String.class);
        SocketAddress socketAddress = SocketAddress.domainSocketAddress(address);

        RequestOptions requestOptions = new RequestOptions()
                .setAbsoluteURI("http://localhost:8080/uds-client-test")
                .setServer(socketAddress);

        HttpClientResponse response = httpClient.request(requestOptions).onItem().transformToUni(req -> {
            return req.send().onItem().transformToUni(resp -> resp.body().replaceWith(resp));
        }).await().atMost(Duration.ofSeconds(10));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.bodyAndAwait().toString()).isEqualTo("Unix Domain Socket Test");
    }

    @AfterEach
    void tearDown() {
        if (httpClient != null) {
            httpClient.close().await().atMost(Duration.ofSeconds(10));
        }
        if (vertx != null) {
            vertx.close().await().atMost(Duration.ofSeconds(10));
        }
    }
}
