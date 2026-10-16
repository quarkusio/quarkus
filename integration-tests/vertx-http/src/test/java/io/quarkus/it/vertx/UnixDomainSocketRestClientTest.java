package io.quarkus.it.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithTestResource(UnixDomainSocketTestResource.class)
@DisabledOnOs(OS.WINDOWS)
public class UnixDomainSocketRestClientTest {

    @RestClient
    UdsClient client;

    @RestClient
    UdsNoUrlClient noUrlClient;

    @Test
    public void testRestClientOverDomainSocket() {
        String result = client.test();
        assertThat(result).isEqualTo("Unix Domain Socket Test");
    }

    @Test
    public void testRestClientOverDomainSocketWithoutUrl() {
        String result = noUrlClient.test();
        assertThat(result).isEqualTo("Unix Domain Socket Test");
    }

    @Test
    public void testProgrammaticRestClientOverDomainSocketWithoutUrl() {
        String address = ConfigProvider.getConfig().getValue("quarkus.http.domain-socket", String.class);
        UdsClient programmaticClient = QuarkusRestClientBuilder.newBuilder()
                .domainSocket(address)
                .build(UdsClient.class);
        String result = programmaticClient.test();
        assertThat(result).isEqualTo("Unix Domain Socket Test");
    }
}
