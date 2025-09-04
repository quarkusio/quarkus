package io.quarkus.hibernate.orm.panache.deployment.test.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.hibernate.orm.panache.deployment.test.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigDefaultPUDatasourceUrlMissingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application-test.properties")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "")
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .assertException(e -> assertThat(e)
                    // Can't use isInstanceOf due to weird classloading in tests
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Persistence unit '<default>' was deactivated automatically because its datasource '<default>' was deactivated",
                            "Datasource '<default>' was deactivated automatically because its URL is not set.",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate the datasource, set configuration property 'quarkus.datasource.jdbc.url'.",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Inject
    MyBean myBean;

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

    @ApplicationScoped
    public static class MyBean {
        @Transactional
        public void useHibernate() {
            MyEntity.findById(1L);
        }
    }
}
