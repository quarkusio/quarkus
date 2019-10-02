package io.quarkus.creator.phase.runnerjar.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsJar;
import io.quarkus.creator.AppCreator;
import io.quarkus.creator.phase.runnerjar.RunnerJarOutcome;

/**
 * Tests uber jar generation through {@link io.quarkus.creator.phase.runnerjar.RunnerJarPhase}
 */
public class UberRunnerJarOutcomeTest extends CreatorOutcomeTestBase {

    private static final Set<String> SIGNATURE_FILES = new HashSet<>();

    static {
        SIGNATURE_FILES.add("META-INF/signature.SF");
        SIGNATURE_FILES.add("META-INF/signature.DSA");
        SIGNATURE_FILES.add("META-INF/signature.RSA");
        SIGNATURE_FILES.add("META-INF/signature.EC");
    }

    @Override
    protected void initProps(final Properties props) {
        super.initProps(props);
        // enable uber jar
        props.put("runner-jar.uber-jar", "true");
    }

    @Override
    protected TsArtifact modelApp() {
        // generate a jar which has files that are considered signature files.
        // note that we do not add real signatures to the jar and instead
        // we just use the well-known signature filename extensions, in this test
        final TsJar signedJar = new TsJar();
        // add the signature files
        for (final String sigFile : SIGNATURE_FILES) {
            signedJar.addEntry("content doesn't matter, just the file name extension does", sigFile);
        }
        // add some random files
        signedJar.addEntry("some file", "a/b/c/d/e.txt");
        signedJar.addEntry("some file", "a/b/foo.SF");
        final TsArtifact signedArtifact = TsArtifact.jar("signed-dep");
        signedArtifact.setContent(signedJar);

        final TsJar regularJar = new TsJar();
        regularJar.addEntry("hello world", "META-INF/helloworld.txt");
        regularJar.addEntry("hello world2", "a/helloworld2.txt");
        final TsArtifact regularArtifact = TsArtifact.jar("regular-dep");
        regularArtifact.setContent(regularJar);

        final TsArtifact appJar = TsArtifact.jar("app").addDependency(signedArtifact).addDependency(regularArtifact);
        return appJar;
    }

    @Override
    protected void testCreator(final AppCreator creator) throws Exception {
        final RunnerJarOutcome outcome = creator.resolveOutcome(RunnerJarOutcome.class);

        final Path runnerJar = outcome.getRunnerJar();
        assertTrue(Files.exists(runnerJar), "Runner jar " + runnerJar + " is missing");
        try (final JarFile jar = new JarFile(runnerJar.toFile())) {
            // verify the signature files are absent
            for (final String sigFile : SIGNATURE_FILES) {
                assertNull(jar.getEntry(sigFile), sigFile + " was expected to be absent in the uberjar, but is present");
            }
            // other files should be present
            assertNotNull(jar.getEntry("a/b/c/d/e.txt"), "a/b/c/d/e.txt is missing from uberjar");
            assertNotNull(jar.getEntry("a/b/foo.SF"), "a/b/foo.SF is missing from uberjar");

            assertNotNull(jar.getEntry("a/helloworld2.txt"), "a/helloworld2.txt is missing from uberjar");
            assertNotNull(jar.getEntry("META-INF/helloworld.txt"), "META-INF/helloworld.txt is missing from uberjar");

        }

    }
}
