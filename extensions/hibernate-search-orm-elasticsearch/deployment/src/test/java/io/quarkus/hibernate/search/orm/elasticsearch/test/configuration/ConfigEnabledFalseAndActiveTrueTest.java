package io.quarkus.hibernate.search.orm.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigEnabledFalseAndActiveTrueTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(IndexedEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-search-orm.enabled", "false")
            .overrideConfigKey("quarkus.hibernate-search-orm.active", "true")
            .assertException(throwable -> assertThat(throwable).isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "Hibernate Search activated explicitly for persistence unit '<default>', but the Hibernate Search extension was disabled at build time",
                            "If you want Hibernate Search to be active for this persistence unit, you must set 'quarkus.hibernate-search-orm.enabled' to 'true' at build time",
                            "If you don't want Hibernate Search to be active for this persistence unit, you must leave 'quarkus.hibernate-search-orm.active' unset or set it to 'false'"));

    @Test
    public void test() {
        // Startup should fail
    }
}
