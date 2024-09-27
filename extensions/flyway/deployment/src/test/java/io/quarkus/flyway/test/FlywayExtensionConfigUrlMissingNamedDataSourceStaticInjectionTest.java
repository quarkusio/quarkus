package io.quarkus.flyway.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionConfigUrlMissingNamedDataSourceStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.users.db-kind", "h2")
            // We need this otherwise the *default* datasource may impact this test
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.username", "sa")
            .overrideConfigKey("quarkus.datasource.password", "sa")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    "jdbc:h2:tcp://localhost/mem:test-quarkus-migrate-at-start;DB_CLOSE_DELAY=-1")
            .assertException(e -> assertThat(e)
                    // Can't use isInstanceOf due to weird classloading in tests
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Flyway for datasource 'users' was deactivated automatically because this datasource was deactivated.",
                            "Datasource 'users' was deactivated automatically because its URL is not set.",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate the datasource, set configuration property 'quarkus.datasource.\"users\".jdbc.url'.",
                            "This bean is injected into",
                            MyBean.class.getName() + "#flyway"));

    @Inject
    MyBean myBean;

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        @FlywayDataSource("users")
        Flyway flyway;

        public void useFlyway() {
            flyway.getConfiguration();
        }
    }
}
