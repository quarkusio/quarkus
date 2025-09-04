package io.quarkus.hibernate.search.orm.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigActiveFalseDynamicInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-search-orm.active", "false");

    @Test
    public void searchMapping() {
        SearchMapping searchMapping = Arc.container().instance(SearchMapping.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether HSearch will be active at runtime.
        // So the bean cannot be null.
        assertThat(searchMapping).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> searchMapping.allIndexedEntities())
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Hibernate Search for persistence unit '<default>' was deactivated through configuration properties",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate Hibernate Search, set configuration property 'quarkus.hibernate-search-orm.active' to 'true'");

        // Hibernate Search APIs also throw exceptions,
        // even if the messages are less explicit (we can't really do better).
        assertThatThrownBy(() -> Search.mapping(Arc.container().instance(SessionFactory.class).get()))
                .isInstanceOf(SearchException.class)
                .hasMessageContaining("Hibernate Search was not initialized");
    }

    @Test
    public void searchSession() {
        SearchSession searchSession = Arc.container().instance(SearchSession.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether HSearch will be active at runtime.
        // So the bean cannot be null.
        assertThat(searchSession).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> searchSession.search(IndexedEntity.class))
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Hibernate Search for persistence unit '<default>' was deactivated through configuration properties",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate Hibernate Search, set configuration property 'quarkus.hibernate-search-orm.active' to 'true'");

        // Hibernate Search APIs also throw exceptions,
        // even if the messages are less explicit (we can't really do better).
        try (Session session = Arc.container().instance(SessionFactory.class).get().openSession()) {
            assertThatThrownBy(() -> Search.session(session).search(IndexedEntity.class))
                    .isInstanceOf(SearchException.class)
                    .hasMessageContaining("Hibernate Search was not initialized");
        }
    }
}
