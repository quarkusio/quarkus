package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class NativeImageBuildStepFailureMessageTest {

    @Test
    void includesTheExitCode() {
        assertThat(NativeImageBuildStep.describeFailure(20, false)).contains("exit code 20");
    }

    @Test
    void mapsKnownGraalVmExitStatusCodes() {
        assertThat(NativeImageBuildStep.describeFailure(20, false)).contains("driver error");
        assertThat(NativeImageBuildStep.describeFailure(30, false)).contains("watchdog");
        assertThat(NativeImageBuildStep.describeFailure(172, false)).contains("reachability metadata");
        assertThat(NativeImageBuildStep.describeFailure(1, false)).contains("builder reported an error");
        assertThat(NativeImageBuildStep.describeFailure(2, false)).contains("fallback image");
        assertThat(NativeImageBuildStep.describeFailure(125, false)).contains("build container");
    }

    @Test
    void decodesSignalDerivedCodes() {
        assertThat(NativeImageBuildStep.describeFailure(139, false)).contains("SIGSEGV");
        assertThat(NativeImageBuildStep.describeFailure(134, false)).contains("SIGABRT");
        assertThat(NativeImageBuildStep.describeFailure(143, false)).contains("SIGTERM");
    }

    @Test
    void javaHeapOomSuggestsRaisingXmx() {
        // Exit code 3 is an in-process Java-heap OOM: raising the builder heap is the correct fix.
        assertThat(NativeImageBuildStep.describeFailure(3, false)).contains("Java heap")
                .contains("Increase the maximum heap size")
                .contains("quarkus.native.native-image-xmx");
    }

    @Test
    void oomKillDoesNotAdviseRaisingXmx() {
        // Exit code 137 is an OOM *kill* (SIGKILL): memory was exhausted, so making more memory available is the
        // fix and raising the builder heap would make a kill more likely. It must not be confused with a Java-heap OOM.
        String msg = NativeImageBuildStep.describeFailure(137, false);
        assertThat(msg).contains("out of memory")
                .contains("Free up memory")
                .contains("more likely to be killed")
                .doesNotContain("Java heap");
    }

    @Test
    void oomKillGuidanceIsShownForNonContainerBuilds() {
        // Regression guard: a plain (non-container) build killed by the kernel OOM-killer must still get the
        // "free up memory" guidance, not only container builds.
        assertThat(NativeImageBuildStep.describeFailure(137, false)).contains("Free up memory");
    }

    @Test
    void containerHintsOnlyForContainerBuilds() {
        // The shared guidance never mentions containers; only a container build gets the extra hint.
        assertThat(NativeImageBuildStep.describeFailure(137, false)).doesNotContain("container");
        assertThat(NativeImageBuildStep.describeFailure(137, true)).contains("container");
    }

    @Test
    void unknownCodeStillPointsAtTheOutput() {
        String msg = NativeImageBuildStep.describeFailure(200, false);
        assertThat(msg).contains("exit code 200").contains("native-image output above");
    }
}
