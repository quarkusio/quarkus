package io.quarkus.hibernate.reactive.panache.test.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.test.MyEntity;
import io.quarkus.hibernate.reactive.panache.test.MyEntityRepository;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigNamedPUDatasourceUrlMissingRepositoryTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class, MyEntityRepository.class))
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
    MyEntityRepository repo;

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

}
