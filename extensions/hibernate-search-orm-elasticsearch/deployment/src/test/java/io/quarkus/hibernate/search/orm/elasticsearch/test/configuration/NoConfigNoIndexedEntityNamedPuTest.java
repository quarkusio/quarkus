package io.quarkus.hibernate.search.orm.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.test.QuarkusUnitTest;

public class NoConfigNoIndexedEntityNamedPuTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class))
            .withConfigurationResource("application-nohsearchconfig-named-pu.properties");

    // When having no indexed entities, no configuration, no datasource,
    // as long as the Hibernate Search beans are not injected anywhere,
    // we should still be able to start the application.
    @Test
    public void testBootSucceedsButHibernateSearchDeactivated() {
        // ... but Hibernate Search's beans should not be available.
        assertThat(Arc.container().instance(SearchMapping.class, new PersistenceUnit.PersistenceUnitLiteral("PU1")).get())
                .isNull();
    }
}
