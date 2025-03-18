package io.quarkus.liquibase.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

public class LiquibaseExtensionConfigUrlMissingDefaultDatasourceStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .assertException(e -> assertThat(e)
                    // Can't use isInstanceOf due to weird classloading in tests
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Liquibase for datasource '<default>' was deactivated automatically because this datasource was deactivated.",
                            "Datasource '<default>' was deactivated automatically because its URL is not set.",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate the datasource, set configuration property 'quarkus.datasource.jdbc.url'.",
                            "Refer to https://quarkus.io/guides/datasource for guidance.",
                            "This bean is injected into",
                            MyBean.class.getName() + "#liquibase"));

    @Inject
    MyBean myBean;

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

    @Test
    @DisplayName("If the URL is missing for the default datasource, the application should boot, but Liquibase should be deactivated for that datasource")
    public void testBootSucceedsButLiquibaseDeactivated() {
        assertThatThrownBy(() -> myBean.useLiquibase())
                .cause()
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Liquibase for datasource '<default>' was deactivated automatically because this datasource was deactivated",
                        "Datasource '<default>' was deactivated automatically because its URL is not set.",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the datasource, set configuration property 'quarkus.datasource.jdbc.url'.",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        LiquibaseFactory liquibase;

        public void useLiquibase() {
            liquibase.getConfiguration();
        }
    }
}
