package io.quarkus.hibernate.search.orm.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.Bindable;

import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.PersistenceUnit.PersistenceUnitLiteral;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that Hibernate Search is able to start in a named PU with a named backend,
 * even when there is no runtime configuration for that backend.
 * <p>
 * This case is interesting mainly because it would most likely lead to an NPE
 * unless we take extra care to avoid it.
 */
@Disabled("The (build-time) 'version' property is not detected for some reason; I suspect a bug in Quarkus Config when there are two levels of maps in the config.")
public class NoRuntimeConfigNamedPUNamedBackendTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(IndexedEntityInNamedBackend.class))
            .overrideConfigKey("quarkus.datasource.\"ds-1\".db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.\"ds-1\".jdbc.url", "jdbc:h2:mem:test")
            .overrideConfigKey("quarkus.hibernate-orm.\"pu-1\".datasource", "ds-1")
            .overrideConfigKey("quarkus.hibernate-orm.\"pu-1\".packages",
                    IndexedEntityInNamedBackend.class.getPackage().getName())
            // For Hibernate Search, provide only build-time config; rely on defaults for everything else.
            .overrideConfigKey("quarkus.hibernate-search-orm.\"pu-1\".elasticsearch.\"mybackend\".version", "7")
            // Disable dev services, since they could interfere with runtime config (in particular the schema management settings).
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            // Make runtime config accessible through CDI
            .overrideConfigKey("quarkus.arc.unremovable-types", HibernateSearchElasticsearchRuntimeConfig.class.getName());

    @Test
    public void test() {
        // Check that runtime config for our PU is indeed missing.
        HibernateSearchElasticsearchRuntimeConfig config = Arc.container()
                .instance(HibernateSearchElasticsearchRuntimeConfig.class).get();
        assertThat(config.persistenceUnits).isNullOrEmpty();
        // Test that Hibernate Search was started correctly regardless.
        SearchMapping mapping = Arc.container()
                .instance(SearchMapping.class, new PersistenceUnitLiteral("pu-1")).get();
        assertThat(mapping).isNotNull();
        assertThat(mapping.allIndexedEntities())
                .extracting(SearchIndexedEntity::javaClass)
                .containsExactlyInAnyOrder((Class) IndexedEntityInNamedBackend.class);
    }
}
