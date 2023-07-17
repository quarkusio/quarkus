package io.quarkus.hibernate.search.orm.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigEnabledFalseAndIndexedEntityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-search-orm.enabled", "false");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void searchMapping() {
        // The bean is not defined during static init, so it's null.
        assertThat(Arc.container().instance(SearchMapping.class).get())
                .isNull();

        // Hibernate Search APIs throw exceptions,
        // even if the messages are not very explicit (we can't really do better).
        assertThatThrownBy(() -> Search.mapping(sessionFactory))
                .isInstanceOf(SearchException.class)
                .hasMessageContaining("Hibernate Search was not initialized");
    }

    @Test
    public void searchSession() {
        // The bean is not defined during static init, so it's null.
        assertThat(Arc.container().instance(SearchSession.class).get())
                .isNull();

        // Hibernate Search APIs throw exceptions,
        // even if the messages are not very explicit (we can't really do better).
        try (Session session = sessionFactory.openSession()) {
            assertThatThrownBy(() -> Search.session(session).search(IndexedEntity.class))
                    .isInstanceOf(SearchException.class)
                    .hasMessageContaining("Hibernate Search was not initialized");
        }
    }
}
