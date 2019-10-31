package io.quarkus.deployment.pkg;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ManifestConfig {

    /**
     * If the Implementation information should be included in the runner jar's MANIFEST.MF.
     */
    @ConfigItem(defaultValue = "true")
    public boolean addImplementationEntries;

}
