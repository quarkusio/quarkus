package io.quarkus.hibernate.search.standalone.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
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
            .overrideConfigKey("quarkus.hibernate-search-standalone.active", "false");

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
                        "Hibernate Search Standalone was deactivated through configuration properties",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate Hibernate Search Standalone, set configuration property 'quarkus.hibernate-search-standalone.active' to 'true'");
    }

}
