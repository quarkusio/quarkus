package io.quarkus.hibernate.search.standalone.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.inject.CreationException;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigActiveFalseAndIndexedEntityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-search-standalone.active", "false");

    @Test
    public void searchMapping() {
        SearchMapping searchMapping = Arc.container().instance(SearchMapping.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether HSearch will be active at runtime.
        // So the bean cannot be null.
        assertThat(searchMapping).isNotNull();
        // However, any attempt to use it at runtime will fail.
        CreationException e = assertThrows(CreationException.class, () -> searchMapping.allIndexedEntities());
        assertThat(e.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContainingAll(
                        "Cannot retrieve the SearchMapping",
                        "Hibernate Search Standalone was deactivated through configuration properties");
    }

}
