package io.quarkus.it.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithTestResource(UnixDomainSocketTestResource.class)
@DisabledOnOs(OS.WINDOWS)
public class UnixDomainSocketRestClientTest {

    @RestClient
    UdsClient client;

    @Test
    public void testRestClientOverDomainSocket() {
        String result = client.test();
        assertThat(result).isEqualTo("Unix Domain Socket Test");
    }
}
