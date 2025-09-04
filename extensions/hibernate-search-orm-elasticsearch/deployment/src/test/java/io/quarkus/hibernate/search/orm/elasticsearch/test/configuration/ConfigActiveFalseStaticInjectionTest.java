package io.quarkus.hibernate.search.orm.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigActiveFalseStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-search-orm.active", "false")
            .assertException(e -> assertThat(e)
                    .hasMessageContainingAll(
                            "Hibernate Search for persistence unit '<default>' was deactivated through configuration properties",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate Hibernate Search, set configuration property 'quarkus.hibernate-search-orm.active' to 'true'",
                            "This bean is injected into",
                            ConfigActiveFalseStaticInjectionTest.class.getName() + "#searchSession"));

    @Inject
    SearchSession searchSession;

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }
}
