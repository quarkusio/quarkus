package io.quarkus.hibernate.search.standalone.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigEnabledFalseAndActiveTrueTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-search-standalone.enabled", "false")
            .overrideConfigKey("quarkus.hibernate-search-standalone.active", "true")
            .assertException(throwable -> assertThat(throwable)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "Hibernate Search Standalone activated explicitly, but the Hibernate Search Standalone extension was disabled at build time",
                            "If you want Hibernate Search Standalone to be active, you must set 'quarkus.hibernate-search-standalone.enabled' to 'true' at build time",
                            "If you don't want Hibernate Search Standalone to be active, you must leave 'quarkus.hibernate-search-standalone.active' unset or set it to 'false'"));

    @Test
    public void test() {
        // Startup should fail
    }
}
