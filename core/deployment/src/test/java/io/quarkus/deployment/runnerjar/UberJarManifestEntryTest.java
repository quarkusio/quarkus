package io.quarkus.deployment.runnerjar;

import java.util.Properties;

/**
 * Verifies that the uber-jar generated Jar has a META-INF/MANIFEST.MF containing the
 * matching Add-Opens entries from the ModuleOpenBuildItem(s) defined by all available extensions.
 */
public class UberJarManifestEntryTest extends FastJarManifestEntryTest {
    @Override
    protected Properties buildSystemProperties() {
        var props = new Properties();
        props.setProperty("quarkus.package.jar.type", "uber-jar");
        return props;
    }
}
