package io.quarkus.cyclonedx.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class EmbeddedSbomCustomResourceNameTest {

    private static final String CUSTOM_RESOURCE_NAME = "META-INF/custom-sbom.json";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(EmbeddedSbomCustomResourceNameTest.class))
            .overrideConfigKey("quarkus.cyclonedx.embedded.enabled", "true")
            .overrideConfigKey("quarkus.cyclonedx.embedded.resource-name", CUSTOM_RESOURCE_NAME);

    @Test
    public void embeddedSbomIsStoredUnderTheCustomResourceName() throws Exception {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        // the default resource name must not be used when a custom one is configured
        assertThat(cl.getResource("META-INF/sbom/dependency.cdx.json")).isNull();

        try (InputStream is = cl.getResourceAsStream(CUSTOM_RESOURCE_NAME)) {
            assertThat(is).as("embedded dependency SBOM resource").isNotNull();
            final Bom bom = new JsonParser().parse(is);
            assertThat(bom).isNotNull();
            assertThat(bom.getComponents()).isNotEmpty();
        }
    }
}
