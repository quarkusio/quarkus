package io.quarkus.annotation.processor.generate_doc;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class GenerateExtensionConfigurationDocTest {

    private GenerateExtensionConfigurationDoc generateExtensionConfigurationDoc;

    @Before
    public void setup() {
        generateExtensionConfigurationDoc = new GenerateExtensionConfigurationDoc();
    }

    @Test
    public void shouldReturnConfigRootName() {
        String configRoot = "org.acme.ConfigRoot";
        String expected = "org.acme.ConfigRoot.adoc";
        String fileName = generateExtensionConfigurationDoc.computeExtensionDocFileName(configRoot);
        assertEquals(expected, fileName);
    }

    @Test
    public void shouldAddCoreInComputedExtensionName() {
        String configRoot = "io.quarkus.runtime.RuntimeConfig";
        String expected = "quarkus-core-runtime-config.adoc";
        String fileName = generateExtensionConfigurationDoc.computeExtensionDocFileName(configRoot);
        assertEquals(expected, fileName);

        configRoot = "io.quarkus.deployment.BuildTimeConfig";
        expected = "quarkus-core-build-time-config.adoc";
        fileName = generateExtensionConfigurationDoc.computeExtensionDocFileName(configRoot);
        assertEquals(expected, fileName);

        configRoot = "io.quarkus.deployment.path.BuildTimeConfig";
        expected = "quarkus-core-build-time-config.adoc";
        fileName = generateExtensionConfigurationDoc.computeExtensionDocFileName(configRoot);
        assertEquals(expected, fileName);
    }

    @Test
    public void shouldGuessArtifactId() {
        String configRoot = "io.quarkus.agroal.Config";
        String expected = "quarkus-agroal.adoc";
        String fileName = generateExtensionConfigurationDoc.computeExtensionDocFileName(configRoot);
        assertEquals(expected, fileName);

        configRoot = "io.quarkus.keycloak.Config";
        expected = "quarkus-keycloak.adoc";
        fileName = generateExtensionConfigurationDoc.computeExtensionDocFileName(configRoot);
        assertEquals(expected, fileName);

        configRoot = "io.quarkus.extension.name.BuildTimeConfig";
        expected = "quarkus-extension-name.adoc";
        fileName = generateExtensionConfigurationDoc.computeExtensionDocFileName(configRoot);
        assertEquals(expected, fileName);
    }
}
