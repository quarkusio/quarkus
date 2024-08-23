package io.quarkus.flyway.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionConfigEmptyDefaultDatasourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // The datasource won't be truly "unconfigured" if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    Instance<Flyway> flywayForDefaultDatasource;

    @Inject
    MyBean myBean;

    @Test
    @DisplayName("If there is no config for the default datasource, the application should boot, but Flyway should be deactivated for that datasource")
    public void testBootSucceedsButFlywayDeactivated() {
        assertThatThrownBy(flywayForDefaultDatasource::get)
                .isInstanceOf(CreationException.class)
                .cause()
                .hasMessageContainingAll("Unable to find datasource '<default>' for Flyway",
                        "Datasource '<default>' is not configured.",
                        "To solve this, configure datasource '<default>'.",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @Test
    @DisplayName("If there is no config for the default datasource, the application should boot even if we inject a bean that depends on Liquibase, but actually using Liquibase should fail")
    public void testBootSucceedsWithInjectedBeanDependingOnFlywayButFlywayDeactivated() {
        assertThatThrownBy(() -> myBean.useFlyway())
                .cause()
                .hasMessageContainingAll("Unable to find datasource '<default>' for Flyway",
                        "Datasource '<default>' is not configured.",
                        "To solve this, configure datasource '<default>'.",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        Flyway flywayForDefaultDatasource;

        public void useFlyway() {
            flywayForDefaultDatasource.getConfiguration();
        }
    }
}
