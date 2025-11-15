package io.quarkus.hibernate.reactive.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.reactive.config.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigNamedPUDatasourceUrlMissingDynamicInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application-config-named-pu.properties")
            .overrideConfigKey("quarkus.datasource.\"mydatasource\".reactive.url", "")
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .assertException(e -> assertThat(e)
                    .hasMessageContainingAll(
                            "Unable to find datasource 'mydatasource' for persistence unit 'mypu'",
                            "Datasource 'mydatasource' was deactivated automatically because its URL is not set.",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate the datasource, set configuration property 'quarkus.datasource.\"mydatasource\".reactive.url'",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Inject
    @PersistenceUnit("mypu")
    InjectableInstance<Mutiny.SessionFactory> sessionFactory;

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }
}
