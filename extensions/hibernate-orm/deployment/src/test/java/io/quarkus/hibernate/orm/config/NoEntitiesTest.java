package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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

    @Inject
    private SessionFactory sessionFactory;

    @Inject
    private Session session;

    @Inject
    private StatelessSession statelessSession;

    @Test
    public void testNoEntities() {
        assertThat(sessionFactory.getMetamodel().getEntities()).isEmpty();
    }

    @Test
    @Transactional
    public void testSessionNativeQuery() {
        assertThat(session.createNativeQuery("select 1", Long.class).getResultList())
                .containsExactly(1L);
    }

    @Test
    @Transactional
    public void testStatelessSessionNativeQuery() {
        assertThat(statelessSession.createNativeQuery("select 1", Long.class).getResultList())
                .containsExactly(1L);
    }

}
