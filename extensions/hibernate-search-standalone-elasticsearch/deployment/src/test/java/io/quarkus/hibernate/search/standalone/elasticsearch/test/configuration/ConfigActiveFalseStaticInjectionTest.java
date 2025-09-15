package io.quarkus.hibernate.search.standalone.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
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
            .overrideConfigKey("quarkus.hibernate-search-standalone.active", "false")
            .assertException(e -> assertThat(e)
                    .hasMessageContainingAll(
                            "Hibernate Search Standalone was deactivated through configuration properties",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate Hibernate Search Standalone, set configuration property 'quarkus.hibernate-search-standalone.active' to 'true'",
                            "This bean is injected into",
                            ConfigActiveFalseStaticInjectionTest.class.getName() + "#searchMapping"));
    @Inject
    SearchMapping searchMapping;

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

}
