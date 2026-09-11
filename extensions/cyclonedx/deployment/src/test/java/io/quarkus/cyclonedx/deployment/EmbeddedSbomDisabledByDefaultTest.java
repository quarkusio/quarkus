package io.quarkus.cyclonedx.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class EmbeddedSbomDisabledByDefaultTest {

    // quarkus.cyclonedx.embedded.enabled is not set, so no SBOM should be embedded
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(EmbeddedSbomDisabledByDefaultTest.class));

    @Test
    public void noEmbeddedSbomResourceByDefault() {
        assertThat(Thread.currentThread().getContextClassLoader()
                .getResource("META-INF/sbom/dependency.cdx.json"))
                .as("embedded dependency SBOM resource")
                .isNull();
    }
}
