package io.quarkus.elasticsearch.javaclient.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Identifier;

public class ActiveInactiveClientTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("active-inactive.properties", "application.properties")
                    .addClass(SillyService.class));

    // So that we reference the clients somewhere ...
    @ApplicationScoped
    public static class SillyService {
        @Inject
        InjectableInstance<RestClient> restClient1;
        @Inject
        @Identifier("client2")
        InjectableInstance<RestClient> restClient2;
        @Inject
        @Identifier("client3")
        InjectableInstance<RestClient> restClient3;
    }

    @Test
    void smoke() {
        // low level clients should be there as well
        assertTrue(Arc.container().select(RestClient.class).getHandle().getBean().isActive());
        assertFalse(
                Arc.container().select(RestClient.class, Identifier.Literal.of("client2")).getHandle().getBean().isActive());
        assertTrue(Arc.container().select(RestClient.class, Identifier.Literal.of("client3")).getHandle().getBean().isActive());
        // as the Java clients:
        assertTrue(Arc.container().select(ElasticsearchClient.class).getHandle().getBean().isActive());
        assertFalse(Arc.container().select(ElasticsearchClient.class, Identifier.Literal.of("client2")).getHandle().getBean()
                .isActive());
        assertFalse(Arc.container().select(ElasticsearchClient.class, Identifier.Literal.of("client3")).getHandle().getBean()
                .isActive());
    }
}
