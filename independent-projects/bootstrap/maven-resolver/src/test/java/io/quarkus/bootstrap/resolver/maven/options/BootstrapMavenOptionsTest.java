package io.quarkus.bootstrap.resolver.maven.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.cli.CLIManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BootstrapMavenOptionsTest {
    private static String toRestore;

    @BeforeAll
    static void recordPreviousCmdLineProperty() {
        toRestore = System.getProperty(BootstrapMavenOptions.QUARKUS_INTERNAL_MAVEN_CMD_LINE_ARGS);
    }

    @AfterAll
    static void restorePreviousCmdLineProperty() {
        if (toRestore != null) {
            System.setProperty(BootstrapMavenOptions.QUARKUS_INTERNAL_MAVEN_CMD_LINE_ARGS, toRestore);
        } else {
            System.clearProperty(BootstrapMavenOptions.QUARKUS_INTERNAL_MAVEN_CMD_LINE_ARGS);
        }
    }

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

    private BootstrapMavenOptions parseOptions(String line) {
        return BootstrapMavenOptions.newInstance(line);
    }

    @Test
    void updateMavenCmdLineAndExtractUserProperties() {
        Map<Object, Object> props = new HashMap<>();
        props.put("quarkus.args", "get -u foo");
        props.put("foo", "bar boo");
        props.put("empty", "");
        props.put("BOOL", "true");
        final List<String> got = BootstrapMavenOptions.updateMavenCmdLineAndExtractUserProperties("quarkus:dev " +
                "-Dquarkus.args=get -u foo " +
                "-Dfoo=bar boo -x -DBOOL -Dempty=", props);
        final String[] expect = { "-Dquarkus.args=\"get -u foo\"", "-Dfoo=\"bar boo\"", "-DBOOL", "-Dempty=\"\"" };
        assertEquals(Arrays.asList(expect), got);
        assertEquals("quarkus:dev -Dquarkus.args=\"get -u foo\" -Dfoo=\"bar boo\" -x -DBOOL -Dempty=\"\"",
                BootstrapMavenOptions.getMavenCmdLine());

    }

    @Test
    void defaultQuarkusBuildOptionsShouldWork() {
        Map<Object, Object> props = new HashMap<>();
        props.put("skipTests", "true");
        props.put("skipITs", "true");
        props.put("no-format", "true");
        props.put("documentation-pdf", "true");
        BootstrapMavenOptions.updateMavenCmdLineAndExtractUserProperties(
                "  -e -B -DskipTests -DskipITs -Dno-format -Ddocumentation-pdf clean install", props);
        assertEquals("-e -B -DskipTests -DskipITs -Dno-format -Ddocumentation-pdf clean install",
                BootstrapMavenOptions.getMavenCmdLine());
    }

    @Test
    void quicklyShouldWork() {
        Map<Object, Object> props = new HashMap<>();
        props.put("quickly", "true");
        BootstrapMavenOptions.updateMavenCmdLineAndExtractUserProperties(
                "-Dquickly", props);
        assertEquals("-Dquickly",
                BootstrapMavenOptions.getMavenCmdLine());
    }

    @Test
    void shouldAllowSpacesAfterUserOptionPrefix() {
        String cmd = "-B -D maven.repo.local=/home/runner/.m2/repository -s /tmp/invoker-settings13193518626400815622.xml clean package -Dquarkus.kubernetes.deploy=true";
        Map<Object, Object> props = new HashMap<>();
        props.put("maven.repo.local", "/home/runner/.m2/repository");
        props.put("quarkus.kubernetes.deploy", "true");
        BootstrapMavenOptions.updateMavenCmdLineAndExtractUserProperties(cmd, props);
        assertEquals(
                "-B -Dmaven.repo.local=\"/home/runner/.m2/repository\" -s /tmp/invoker-settings13193518626400815622.xml clean package -Dquarkus.kubernetes.deploy=\"true\"",
                BootstrapMavenOptions.getMavenCmdLine());
    }
}
