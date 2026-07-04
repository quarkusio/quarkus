package io.quarkus.it.http3.autotls;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class Http3RestClientAutoTlsTest {

    @RestClient
    HelloClientTrustAll trustAllClient;

    @RestClient
    HelloClientTrustStore trustStoreClient;

    @Test
    void testRestClientWithTrustAll() {
        assertThat(trustAllClient.version()).isEqualTo("HTTP_3");
    }

    @Test
    void testRestClientWithTrustStore() {
        assertThat(trustStoreClient.version()).isEqualTo("HTTP_3");
    }
}
