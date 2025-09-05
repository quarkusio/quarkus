package io.quarkus.hibernate.reactive.panache.test.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.hibernate.reactive.panache.test.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigDefaultPUDatasourceActiveFalseTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.active", "false")
            .assertException(e -> assertThat(e)
                    // Can't use isInstanceOf due to weird classloading in tests
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Persistence unit '<default>' was deactivated automatically because its datasource '<default>' was deactivated",
                            "Datasource '<default>' was deactivated through configuration properties",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate the datasource, set configuration property 'quarkus.datasource.active'"
                                    + " to 'true' and configure datasource '<default>'",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }
}
