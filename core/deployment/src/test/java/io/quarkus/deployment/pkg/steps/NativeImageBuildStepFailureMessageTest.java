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
    void outOfMemoryCodesSuggestXmx() {
        assertThat(NativeImageBuildStep.describeFailure(137, false)).contains("out of memory")
                .contains("quarkus.native.native-image-xmx");
        assertThat(NativeImageBuildStep.describeFailure(3, false)).contains("Java heap")
                .contains("quarkus.native.native-image-xmx");
    }

    @Test
    void containerHintsOnlyForContainerBuilds() {
        assertThat(NativeImageBuildStep.describeFailure(137, false)).doesNotContain("cgroup")
                .doesNotContain("container runtime");
    }

    @Test
    void unknownCodeStillPointsAtTheOutput() {
        String msg = NativeImageBuildStep.describeFailure(200, false);
        assertThat(msg).contains("exit code 200").contains("native-image output above");
    }
}
