package io.quarkus.it.rest.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.client.main.CorrelationIdClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.common.vertx.ContextLocals;

@QuarkusTest
public class RestClientInTestMethodWithContextTest {

    @RestClient
    CorrelationIdClient client;

    @RunOnVertxContext(runOnEventLoop = false)
    @Test
    public void test() {
        ContextLocals.put(CorrelationIdClient.CORRELATION_ID_HEADER_NAME, "dummy");
        assertThat(client.get()).isEqualTo("dummy");
    }
}
