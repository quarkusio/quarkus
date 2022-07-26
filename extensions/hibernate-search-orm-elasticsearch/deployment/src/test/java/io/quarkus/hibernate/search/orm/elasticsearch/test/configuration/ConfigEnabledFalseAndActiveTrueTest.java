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
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigEnabledFalseAndActiveTrueTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-search-orm.enabled", "false")
            .overrideConfigKey("quarkus.hibernate-search-orm.active", "true")
            .assertException(throwable -> assertThat(throwable)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining(
                            "Hibernate Search activated explicitly, but Hibernate Search was disabled at build time",
                            "If you want Hibernate Search to be active at runtime, you must set 'quarkus.hibernate-search-orm.enabled' to 'true' at build time",
                            "If you don't want Hibernate Search to be active at runtime, you must leave 'quarkus.hibernate-search-orm.active' unset or set it to 'false'"));

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void test() {
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
