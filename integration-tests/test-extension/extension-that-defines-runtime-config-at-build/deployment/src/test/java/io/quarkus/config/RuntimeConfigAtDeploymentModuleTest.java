package io.quarkus.config;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RuntimeConfigAtDeploymentModuleTest {

    @RegisterExtension
    static QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .assertException(throwable -> {
                assertThat(throwable)
                        .hasMessageContaining(
                        "Configuration classes with ConfigPhase.RUN_TIME or ConfigPhase.BUILD_AND_RUNTIME_FIXED phases, must be on runtime module");
            });

    @Test
    public void shouldThrowsException() {
        fail();
    }

}
