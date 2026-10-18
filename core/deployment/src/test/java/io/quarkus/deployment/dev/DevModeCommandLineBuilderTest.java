package io.quarkus.deployment.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.model.ExtensionDevModeConfig;
import io.quarkus.bootstrap.model.JvmOptions;
import io.quarkus.maven.dependency.ArtifactKey;

public class DevModeCommandLineBuilderTest {

    @TempDir
    File outputDir;

    @Test
    public void extensionSetsJvmOptionWoValue() throws Exception {

        final ExtensionDevModeConfig acmeMagic = new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.acme:acme-magic"),
                JvmOptions.builder()
                        .add("enable-preview")
                        .build(),
                Set.of());

        var args = getCliArguments(acmeMagic);
        assertThat(args).contains("--enable-preview");
    }

    @Test
    public void extensionSetsJvmOptionWithValue() throws Exception {

        final ExtensionDevModeConfig acmeMagic = new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.acme:acme-magic"),
                JvmOptions.builder()
                        .add("enable-native-access", "ALL-UNNAMED")
                        .build(),
                Set.of());

        var args = getCliArguments(acmeMagic);
        assertThat(args).contains("--enable-native-access=ALL-UNNAMED");
    }

    @Test
    public void extensionAddModules() throws Exception {

        final ExtensionDevModeConfig acmeMagic = new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.acme:acme-magic"),
                JvmOptions.builder()
                        .add("add-modules", "jdk.incubator.vector")
                        .addAll("add-modules", List.of("jdk.incubator.vector", "java.management"))
                        .build(),
                Set.of());

        var args = getCliArguments(acmeMagic);
        assertThat(args).contains("--add-modules=java.management,jdk.incubator.vector");
    }

    @Test
    public void extensionAddOpens() throws Exception {

        final ExtensionDevModeConfig acmeMagic = new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.acme:acme-magic"),
                JvmOptions.builder()
                        .add("add-opens", "java.base/java.util=ALL-UNNAMED")
                        .add("add-opens", "java.base/java.io=ALL-UNNAMED")
                        .add("add-opens", "java.base/java.nio=ALL-UNNAMED")
                        .build(),
                Set.of());

        var args = getCliArguments(acmeMagic);
        assertThat(args).containsSequence("--add-opens", "java.base/java.io=ALL-UNNAMED");
        assertThat(args).containsSequence("--add-opens", "java.base/java.nio=ALL-UNNAMED");
        assertThat(args).containsSequence("--add-opens", "java.base/java.util=ALL-UNNAMED");
    }

    @Test
    public void extensionEnablingC2() throws Exception {

        var args = getCliArguments();
        // C2 is disabled by default
        assertThat(args).contains("-XX:TieredStopAtLevel=1");

        // extension locking the default value of TieredStopAtLevel
        final ExtensionDevModeConfig acmeMagic = new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.acme:acme-magic"),
                JvmOptions.builder().build(),
                Set.of("TieredStopAtLevel"));
        args = getCliArguments(acmeMagic);
        assertThat(args).doesNotContain("-XX:TieredStopAtLevel=1");

        // user disabling C2 explicitly
        args = getCliBuilder(acmeMagic).forceC2(false).build().getArguments();
        assertThat(args).contains("-XX:TieredStopAtLevel=1");
    }

    @Test
    public void extensionDisablingDebugMode() throws Exception {

        final String agentlibJdwpArg = "-agentlib:jdwp=transport=dt_socket,address=localhost:5005,server=y,suspend=n";

        // in case the debug option is not set, it's expected to be enabled
        var args = getCliBuilder().debug(null).build().getArguments();
        assertThat(args).contains(agentlibJdwpArg);

        // extension locking the default value of -agentlib:jdwp
        final ExtensionDevModeConfig acmeMagic = new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.acme:acme-magic"),
                JvmOptions.builder().build(),
                Set.of("agentlib:jdwp"));
        args = getCliBuilder(acmeMagic).debug(null).build().getArguments();
        assertThat(args).doesNotContain(agentlibJdwpArg);

        // user explicitly enables debug
        args = getCliBuilder(acmeMagic).debug("true").build().getArguments();
        assertThat(args).contains(agentlibJdwpArg);
    }

    @Test
    public void extensionSetsXxBooleanOption() throws Exception {

        // false
        var args = getCliArguments(new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.acme:acme-magic"),
                JvmOptions.builder()
                        .addXxOption("UseThreadPriorities", "false")
                        .build(),
                Set.of()));
        assertThat(args).contains("-XX:-UseThreadPriorities");

        // true
        args = getCliArguments(new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.acme:acme-magic"),
                JvmOptions.builder()
                        .addXxOption("UseThreadPriorities", "true")
                        .build(),
                Set.of()));
        assertThat(args).contains("-XX:+UseThreadPriorities");
    }

    @Test
    public void extensionSetsXxNumericOption() throws Exception {

        var args = getCliArguments(new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.acme:acme-magic"),
                JvmOptions.builder()
                        .addXxOption("AllocatePrefetchStyle", "1")
                        .build(),
                Set.of()));
        assertThat(args).contains("-XX:AllocatePrefetchStyle=1");
    }

    @Test
    public void disableAllExtensionArgs() throws Exception {

        final ExtensionDevModeConfig acmeMagic = new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.acme:acme-magic"),
                JvmOptions.builder()
                        .add("add-opens", "java.base/java.io=ALL-UNNAMED")
                        .add("add-opens", "java.base/java.nio=ALL-UNNAMED")
                        .build(),
                Set.of("TieredStopAtLevel"));

        final ExtensionDevModeConfig otherMagic = new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.other:other-magic"),
                JvmOptions.builder()
                        .add("add-opens", "java.base/java.util=ALL-UNNAMED")
                        .add("add-opens", "java.base/java.io=ALL-UNNAMED")
                        .build(),
                Set.of());

        // all enabled
        var args = getCliArguments(acmeMagic, otherMagic);
        assertThat(args).containsSequence("--add-opens", "java.base/java.io=ALL-UNNAMED");
        assertThat(args).containsSequence("--add-opens", "java.base/java.nio=ALL-UNNAMED");
        assertThat(args).containsSequence("--add-opens", "java.base/java.util=ALL-UNNAMED");
        assertThat(args).doesNotContain("-XX:TieredStopAtLevel=1");

        // all disabled
        final ExtensionDevModeJvmOptionFilter filter = new ExtensionDevModeJvmOptionFilter();
        filter.setDisableAll(true);
        args = getCliBuilder(acmeMagic, otherMagic)
                .extensionDevModeJvmOptionFilter(filter)
                .build().getArguments();
        assertThat(args).doesNotContain("--add-opens");
        assertThat(args).contains("-XX:TieredStopAtLevel=1");
    }

    @Test
    public void disableParticularExtensionArgs() throws Exception {

        final ExtensionDevModeConfig acmeMagic = new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.acme:acme-magic"),
                JvmOptions.builder()
                        .add("add-opens", "java.base/java.io=ALL-UNNAMED")
                        .add("add-opens", "java.base/java.nio=ALL-UNNAMED")
                        .build(),
                Set.of("TieredStopAtLevel"));

        final ExtensionDevModeConfig otherMagic = new ExtensionDevModeConfig(
                ArtifactKey.fromString("org.other:other-magic"),
                JvmOptions.builder()
                        .add("add-opens", "java.base/java.util=ALL-UNNAMED")
                        .add("add-opens", "java.base/java.io=ALL-UNNAMED")
                        .build(),
                Set.of());

        // all enabled
        var args = getCliArguments(acmeMagic, otherMagic);
        assertThat(args).containsSequence("--add-opens", "java.base/java.io=ALL-UNNAMED");
        assertThat(args).containsSequence("--add-opens", "java.base/java.nio=ALL-UNNAMED");
        assertThat(args).containsSequence("--add-opens", "java.base/java.util=ALL-UNNAMED");
        assertThat(args).doesNotContain("-XX:TieredStopAtLevel=1");

        // all disabled
        final ExtensionDevModeJvmOptionFilter filter = new ExtensionDevModeJvmOptionFilter();
        filter.setDisableFor(List.of("org.acme:acme-magic"));
        args = getCliBuilder(acmeMagic, otherMagic)
                .extensionDevModeJvmOptionFilter(filter)
                .build().getArguments();
        assertThat(args).containsSequence("--add-opens", "java.base/java.io=ALL-UNNAMED");
        assertThat(args).containsSequence("--add-opens", "java.base/java.util=ALL-UNNAMED");
        assertThat(args).contains("-XX:TieredStopAtLevel=1");
    }

    private List<String> getCliArguments(ExtensionDevModeConfig... extensionDevModeConfigs) throws Exception {
        return getCliBuilder(extensionDevModeConfigs).build().getArguments();
    }

    private DevModeCommandLineBuilder getCliBuilder(ExtensionDevModeConfig... extensionDevModeConfigs) {
        return DevModeCommandLine.builder("java")
                .applicationName("test")
                .outputDir(outputDir)
                .buildDir(outputDir)
                .debug("false") // disable debug port check
                .extensionDevModeConfig(List.of(extensionDevModeConfigs));
    }
}
