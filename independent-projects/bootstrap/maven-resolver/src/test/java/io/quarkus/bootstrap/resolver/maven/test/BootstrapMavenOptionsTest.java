package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import org.junit.jupiter.api.Test;

public class BootstrapMavenOptionsTest {

    @Test
    public void testOffline() throws Exception {
        assertTrue(parseOptions("clean install -o -X").hasOption(BootstrapMavenOptions.OFFLINE));
    }

    @Test
    public void testUserSettings() throws Exception {
        final BootstrapMavenOptions parseOptions = parseOptions("clean install -o -s custom/settings/file.xml");
        assertTrue(parseOptions.hasOption(BootstrapMavenOptions.ALTERNATE_USER_SETTINGS));
        assertEquals("custom/settings/file.xml", parseOptions.getOptionValue(BootstrapMavenOptions.ALTERNATE_USER_SETTINGS));
    }

    @Test
    public void testGlobalSettings() throws Exception {
        final BootstrapMavenOptions parseOptions = parseOptions("clean install -o -gs custom/settings/file.xml");
        assertTrue(parseOptions.hasOption(BootstrapMavenOptions.ALTERNATE_GLOBAL_SETTINGS));
        assertEquals("custom/settings/file.xml", parseOptions.getOptionValue(BootstrapMavenOptions.ALTERNATE_GLOBAL_SETTINGS));
    }

    @Test
    public void testAlternatePomFile() throws Exception {
        final BootstrapMavenOptions parseOptions = parseOptions("clean install -o -f alternate/pom-file.xml");
        assertTrue(parseOptions.hasOption(BootstrapMavenOptions.ALTERNATE_POM_FILE));
        assertEquals("alternate/pom-file.xml", parseOptions.getOptionValue(BootstrapMavenOptions.ALTERNATE_POM_FILE));
    }

    @Test
    public void testSupressSnapshotUpdates() throws Exception {
        final BootstrapMavenOptions parseOptions = parseOptions("clean install -nsu");
        assertTrue(parseOptions.hasOption(BootstrapMavenOptions.SUPRESS_SNAPSHOT_UPDATES));
    }

    @Test
    public void testUpdateSnapshots() throws Exception {
        final BootstrapMavenOptions parseOptions = parseOptions("clean install -U");
        assertTrue(parseOptions.hasOption(BootstrapMavenOptions.UPDATE_SNAPSHOTS));
    }

    @Test
    public void testChecksumFailurePolicy() throws Exception {
        final BootstrapMavenOptions parseOptions = parseOptions("clean install -C");
        assertTrue(parseOptions.hasOption(BootstrapMavenOptions.CHECKSUM_FAILURE_POLICY));
    }

    @Test
    public void testChecksumWarningPolicy() throws Exception {
        final BootstrapMavenOptions parseOptions = parseOptions("clean install -c");
        assertTrue(parseOptions.hasOption(BootstrapMavenOptions.CHECKSUM_WARNING_POLICY));
    }

    private BootstrapMavenOptions parseOptions(String line) {
        return BootstrapMavenOptions.newInstance(line);
    }
}
