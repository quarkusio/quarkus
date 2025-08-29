package io.quarkus.hibernate.reactive.panache.test.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.test.MyEntity;
import io.quarkus.hibernate.reactive.panache.test.MyEntityRepository;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigDefaultPUDatasourceActiveFalseRepositoryTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class, MyEntityRepository.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.active", "false")
            .assertException(e -> assertThat(e)
                    .hasMessageContainingAll(
                            "Unable to find datasource '<default>' for persistence unit '<default>'",
                            "Datasource '<default>' was deactivated through configuration properties.",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate the datasource, set configuration property 'quarkus.datasource.active'"
                                    + " to 'true' and configure datasource '<default>'",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Inject
    MyEntityRepository repo;

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

}
