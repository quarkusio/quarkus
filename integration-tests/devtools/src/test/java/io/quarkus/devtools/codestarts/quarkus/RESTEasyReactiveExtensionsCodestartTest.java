package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getCodestartName;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.isUnlisted;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.PlatformAwareTestBase;
import io.quarkus.registry.catalog.Extension;

public class RESTEasyReactiveExtensionsCodestartTest extends PlatformAwareTestBase {

    @Test
    void checkCodestart() {
        for (Extension extension : getExtensionsCatalog().getExtensions()) {
            if (extension.getArtifact().getArtifactId().startsWith("quarkus-resteasy-reactive") && !isUnlisted(extension)) {
                Assertions.assertThat(getCodestartName(extension)).as("Codestart is defined for " + extension.managementKey())
                        .isNotBlank();
            }
        }
    }
}
