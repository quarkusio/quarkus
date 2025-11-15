package io.quarkus.hibernate.orm.panache.deployment.test.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.test.QuarkusUnitTest;

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
    @Transactional
    public void testSessionNativeQuery() {
        assertThat(Panache.getSession().createNativeQuery("select 1", Long.class).getResultList())
                .containsExactly(1L);
    }

}
