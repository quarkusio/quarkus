package io.quarkus.deployment.pkg;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ManifestConfig {

    /**
     * If the Implementation information should be included in the runner jar's MANIFEST.MF.
     */
    @ConfigItem(defaultValue = "true")
    public boolean addImplementationEntries;

    /**
     * Custom manifest sections to be added to the MANIFEST.MF file.
     * An example of the user defined property:
     * quarkus.package.manifest.manifest-sections.{Section-Name}.{Entry-Key1}={Value1}
     * quarkus.package.manifest.manifest-sections.{Section-Name}.{Entry-Key2}={Value2}
     */
    @ConfigItem()
    public Map<String, Map<String, String>> manifestSections;
}
