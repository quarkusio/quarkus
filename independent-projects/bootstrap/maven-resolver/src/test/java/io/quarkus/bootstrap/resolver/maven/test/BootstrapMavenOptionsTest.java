package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import java.util.Properties;
import org.apache.maven.cli.CLIManager;
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

    @Test
    public void testBatchMode() {
        assertEquals("" + CLIManager.BATCH_MODE, BootstrapMavenOptions.BATCH_MODE);
        assertTrue(parseOptions("clean install -B").hasOption(BootstrapMavenOptions.BATCH_MODE));
        assertTrue(parseOptions("clean install --batch-mode").hasOption(BootstrapMavenOptions.BATCH_MODE));
    }

    @Test
    public void testNoTransferProgress() {
        assertEquals(CLIManager.NO_TRANSFER_PROGRESS, BootstrapMavenOptions.NO_TRANSFER_PROGRESS);
        assertTrue(parseOptions("clean install -ntp").hasOption(BootstrapMavenOptions.NO_TRANSFER_PROGRESS));
        assertTrue(parseOptions("clean install --no-transfer-progress").hasOption(BootstrapMavenOptions.NO_TRANSFER_PROGRESS));
    }

    @Test
    public void testSystemPropertiesWithWhitespaces() throws Exception {
        try (AutoCloseable whiteSpace = setProperty("white.space", " value with spaces ");
                AutoCloseable nested = setProperty("nested", "  -Dnested=other ");
                AutoCloseable noValue = setProperty("no-value", "");
                AutoCloseable mix = setProperty("quarkus.args", "get abc -u foo --password foo-bar")) {
            final BootstrapMavenOptions parseOptions = parseOptions("package " + whiteSpace + nested + noValue + " " + mix);
            final Properties userProps = parseOptions.getSystemProperties();
            assertEquals(" value with spaces ", userProps.getProperty("white.space"));
            assertEquals("  -Dnested=other ", userProps.getProperty("nested"));
            assertEquals("", userProps.getProperty("no-value"));
            assertEquals("get abc -u foo --password foo-bar", userProps.getProperty("quarkus.args"));
            assertEquals(4, userProps.size());
        }
    }

    private BootstrapMavenOptions parseOptions(String line) {
        return BootstrapMavenOptions.newInstance(line);
    }

    private static AutoCloseable setProperty(String name, String value) {
        return new AutoCloseable() {
            final boolean clear;
            final String original;
            {
                clear = !System.getProperties().containsKey(name);
                original = System.setProperty(name, value);
            }

            @Override
            public void close() throws Exception {
                if (clear) {
                    System.clearProperty(name);
                } else {
                    System.setProperty(name, original);
                }
            }

            @Override
            public String toString() {
                final StringBuilder buf = new StringBuilder();
                buf.append("-D").append(name);
                if (value != null && !value.isEmpty()) {
                    buf.append('=');
                    buf.append(value);
                }
                return buf.toString();
            }
        };
    }
}
