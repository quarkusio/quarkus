package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
            .withEmptyApplication()
            .withConfigurationResource("application.properties");

    @Inject
    private Mutiny.SessionFactory sessionFactory;

    @Test
    public void testNoEntities() {
        assertThat(sessionFactory.getMetamodel().getEntities()).isEmpty();
    }

    @Test
    @RunOnVertxContext
    public void testSessionNativeQuery(UniAsserter asserter) {
        asserter.assertThat(
                () -> sessionFactory.withTransaction(s -> s.createNativeQuery("select 1", Long.class).getResultList()),
                result -> assertThat(result).containsExactly(1L));
    }

    @Test
    @RunOnVertxContext
    public void testStatelessSessionNativeQuery(UniAsserter asserter) {
        asserter.assertThat(
                () -> sessionFactory.withStatelessTransaction(s -> s.createNativeQuery("select 1", Long.class).getResultList()),
                result -> assertThat(result).containsExactly(1L));
    }

}
