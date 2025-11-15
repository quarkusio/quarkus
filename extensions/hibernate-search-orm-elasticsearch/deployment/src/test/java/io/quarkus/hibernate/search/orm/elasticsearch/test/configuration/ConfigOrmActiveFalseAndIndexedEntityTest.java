package io.quarkus.hibernate.search.orm.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigOrmActiveFalseAndIndexedEntityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.active", "false");

    @Test
    public void test() {
        SessionFactory sessionFactory = Arc.container().instance(SessionFactory.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether Hibernate ORM will be active at runtime.
        // So the bean cannot be null.
        assertThat(sessionFactory).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(sessionFactory::getMetamodel)
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Persistence unit '<default>' was deactivated through configuration properties",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the persistence unit, set configuration property 'quarkus.hibernate-orm.active'"
                                + " to 'true' and configure datasource '<default>'.",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");

        // Same with Hibernate Search beans
        SearchMapping searchMapping = Arc.container().instance(SearchMapping.class).get();
        assertThat(searchMapping).isNotNull();
        assertThatThrownBy(searchMapping::allIndexedEntities)
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Hibernate Search for persistence unit '<default>' was deactivated automatically because the persistence unit was deactivated",
                        "Persistence unit '<default>' was deactivated through configuration properties",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the persistence unit, set configuration property 'quarkus.hibernate-orm.active'"
                                + " to 'true' and configure datasource '<default>'.");
    }
}
