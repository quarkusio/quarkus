package io.quarkus.cyclonedx.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class EmbeddedSbomTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(EmbeddedSbomTest.class))
            .overrideConfigKey("quarkus.cyclonedx.embedded.enabled", "true");

    @Test
    public void embeddedDependencySbomIsOnTheClasspath() throws Exception {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("META-INF/sbom/dependency.cdx.json")) {
            assertThat(is).as("embedded dependency SBOM resource").isNotNull();
            final Bom bom = new JsonParser().parse(is);
            assertThat(bom).isNotNull();
            assertThat(bom.getMetadata()).isNotNull();
            assertThat(bom.getMetadata().getComponent()).isNotNull();
            assertThat(bom.getComponents()).isNotEmpty();
        }
    }
}
