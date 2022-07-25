package io.quarkus.hibernate.search.orm.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;
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

public class ConfigEnabledFalseAndIndexedEntityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-search-orm.enabled", "false");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void test() {
        assertThat(Arc.container().instance(SearchMapping.class).get())
                .isNull();

        assertThatThrownBy(() -> Search.mapping(sessionFactory))
                .isInstanceOf(SearchException.class)
                .hasMessageContaining("Hibernate Search was not initialized.");

        assertThat(Arc.container().instance(SearchSession.class).get())
                .isNull();

        try (Session session = sessionFactory.openSession()) {
            assertThatThrownBy(() -> Search.session(session).search(IndexedEntity.class))
                    .isInstanceOf(SearchException.class)
                    .hasMessageContaining("Hibernate Search was not initialized.");
        }
    }
}
