package io.quarkus.hibernate.search.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.inject.Inject;

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

public class ConfigDisabledAndIndexedEntityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-search-orm.enabled", "false");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void testDisabled() {
        assertThatThrownBy(() -> Arc.container().instance(SearchMapping.class).get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "Cannot retrieve the SearchMapping: Hibernate Search was disabled through configuration properties");

        assertThatThrownBy(() -> Search.mapping(sessionFactory))
                .isInstanceOf(SearchException.class)
                .hasMessageContaining("Hibernate Search was not initialized.");

        assertThatThrownBy(() -> Arc.container().instance(SearchSession.class).get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "Cannot retrieve the SearchSession: Hibernate Search was disabled through configuration properties");

        try (Session session = sessionFactory.openSession()) {
            assertThatThrownBy(() -> Search.session(session).search(IndexedEntity.class))
                    .isInstanceOf(SearchException.class)
                    .hasMessageContaining("Hibernate Search was not initialized.");
        }
    }
}
