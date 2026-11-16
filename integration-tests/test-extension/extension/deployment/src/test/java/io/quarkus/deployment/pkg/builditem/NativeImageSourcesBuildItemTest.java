package io.quarkus.deployment.pkg.builditem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.builder.BuildContext;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestBuildStep;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Verifies that {@link NativeImageSourcesBuildItem} is produced for a native sources build and that it is consistent with
 * the files written to the {@code native-sources} directory.
 */
class NativeImageSourcesBuildItemTest {

    private static final String RESULT_TYPE = "test-native-image-sources";
    private static final String ARGS_SEPARATOR = "\n";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withEmptyApplication()
            .setApplicationName("native-image-sources")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.native.enabled", "true")
            .overrideConfigKey("quarkus.native.sources-only", "true")
            .addBuildChainCustomizerEntries(new QuarkusProdModeTest.BuildChainCustomizerEntry(
                    NativeImageSourcesConsumer.class,
                    List.of(ArtifactResultBuildItem.class),
                    List.of(NativeImageSourcesBuildItem.class)));

    @ProdBuildResults
    ProdModeTestResults prodModeTestResults;

    @Test
    void buildItemMatchesNativeSourcesDirectory() throws IOException {
        ArtifactResult result = prodModeTestResults.getResults().stream()
                .filter(r -> RESULT_TYPE.equals(r.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("NativeImageSourcesBuildItem was not produced"));
        Map<String, String> item = result.getMetadata();

        Path nativeSources = result.getPath();
        assertThat(nativeSources).isDirectory()
                .isEqualTo(prodModeTestResults.getBuildDir().resolve("native-sources"));

        assertThat(item.get("runnerJarName")).isEqualTo("native-image-sources-runner.jar");
        assertThat(nativeSources.resolve(item.get("runnerJarName"))).isRegularFile();
        assertThat(item.get("nativeImageName")).isEqualTo("native-image-sources-runner");

        List<String> args = List.of(item.get("nativeImageArgs").split(ARGS_SEPARATOR));
        assertThat(args).endsWith("-o", item.get("nativeImageName"), "-jar", item.get("runnerJarName"));
        assertThat(Files.readString(nativeSources.resolve("native-image.args"))).isEqualTo(String.join(" ", args));

        assertThat(item.get("graalVMVersion")).isNotBlank()
                .isEqualTo(Files.readString(nativeSources.resolve("graalvm.version")));

        // not a container build
        assertThat(item.get("builderImage")).isEmpty();
        assertThat(nativeSources.resolve("native-builder.image")).doesNotExist();
    }

    /**
     * Simulates an extension consuming {@link NativeImageSourcesBuildItem}. It reports what it saw through an
     * {@link ArtifactResultBuildItem} so that the test can assert on it.
     */
    public static class NativeImageSourcesConsumer extends ProdModeTestBuildStep {

        public NativeImageSourcesConsumer(Map<String, Object> testContext) {
            super(testContext);
        }

        @Override
        public void execute(BuildContext context) {
            NativeImageSourcesBuildItem sources = context.consume(NativeImageSourcesBuildItem.class);
            context.produce(new ArtifactResultBuildItem(sources.getDirectory(), RESULT_TYPE, Map.of(
                    "runnerJarName", sources.getRunnerJarName(),
                    "nativeImageName", sources.getNativeImageName(),
                    "nativeImageArgs", String.join(ARGS_SEPARATOR, sources.getNativeImageArgs()),
                    "graalVMVersion", sources.getGraalVMVersion(),
                    "builderImage", sources.getBuilderImage().orElse(""))));
        }
    }
}
