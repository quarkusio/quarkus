package io.quarkus.hibernate.reactive.panache.test.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Test that a persistence unit without any entities does get started,
 * and can be used, be it only for native queries,
 * as long as a datasource is present.
 */
public class NoEntitiesTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication();

    @Test
    @RunOnVertxContext
    public void testSessionNativeQuery(UniAsserter asserter) {
        asserter.assertThat(() -> Panache.withTransaction(
                () -> Panache.getSession().chain(session -> session.createNativeQuery("select 1", Long.class).getResultList())),
                result -> assertThat(result).containsExactly(1L));
    }

}
