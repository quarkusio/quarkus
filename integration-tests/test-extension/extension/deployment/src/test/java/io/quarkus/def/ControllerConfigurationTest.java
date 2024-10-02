package io.quarkus.def;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.def.ControllerConfiguration;
import io.quarkus.extest.runtime.def.QuarkusControllerConfiguration;
import io.quarkus.test.QuarkusUnitTest;

public class ControllerConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Inject
    ControllerConfiguration controllerConfiguration;

    @Test
    public void test() {
        assertThat(controllerConfiguration).isInstanceOfSatisfying(QuarkusControllerConfiguration.class,
                quarkusControllerConfiguration -> {
                    assertThat(quarkusControllerConfiguration.getName()).isEqualTo("test1");
                    assertThat(quarkusControllerConfiguration.getResourceTypeName()).isEqualTo("test2");
                    assertThat(controllerConfiguration.getNamespaces()).containsOnly("foo", "bar");
                    assertThat(controllerConfiguration.getInformerListLimit()).isEmpty();
                });

    }

}
