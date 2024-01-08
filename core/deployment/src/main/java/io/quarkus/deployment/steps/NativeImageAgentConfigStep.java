package io.quarkus.deployment.steps;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.builder.Json;
import io.quarkus.builder.JsonReader;
import io.quarkus.builder.JsonTransform;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageAgentConfigDirectoryBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

public class NativeImageAgentConfigStep {
    private static final Logger log = Logger.getLogger(NativeImageAgentConfigStep.class);

    private final Pattern resourceSkipPattern;

    public NativeImageAgentConfigStep() {
        resourceSkipPattern = discardPattern("apache.maven", "application.properties", "groovy", "jboss", "junit",
                "logging.properties", "microprofile", "quarkus", "slf4j", "smallrye", "surefire", "Test.class");
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void transformConfig(NativeConfig nativeConfig,
            BuildProducer<NativeImageAgentConfigDirectoryBuildItem> nativeImageAgentConfigDirectoryProducer,
            NativeImageSourceJarBuildItem nativeImageSourceJarBuildItem,
            BuildSystemTargetBuildItem buildSystemTargetBuildItem) throws IOException {
        final Path basePath = buildSystemTargetBuildItem.getOutputDirectory()
                .resolve(Path.of("native-agent-base-config"));
        if (basePath.toFile().exists()) {
            final Path outputDir = nativeImageSourceJarBuildItem.getPath().getParent();
            final String targetDirName = "native-agent-config";
            final Path targetPath = outputDir.resolve(Path.of(targetDirName));
            if (!targetPath.toFile().exists()) {
                targetPath.toFile().mkdirs();
            }
            Files.copy(basePath.resolve("reflect-config.json"), targetPath.resolve("reflect-config.json"));
            Files.copy(basePath.resolve("serialization-config.json"), targetPath.resolve("serialization-config.json"));
            Files.copy(basePath.resolve("jni-config.json"), targetPath.resolve("jni-config.json"));
            Files.copy(basePath.resolve("proxy-config.json"), targetPath.resolve("proxy-config.json"));
            transformJsonObject(basePath, "resource-config.json", targetPath, JsonTransform.dropping(this::discardResource));

            if (log.isInfoEnabled()) {
                log.info("Discovered native image agent generated files");
                logConfigurationFileContents("reflect-config.json", targetPath);
                logConfigurationFileContents("serialization-config.json", targetPath);
                logConfigurationFileContents("jni-config.json", targetPath);
                logConfigurationFileContents("proxy-config.json", targetPath);
                logConfigurationFileContents("resource-config.json", targetPath);
            }

            if (nativeConfig.agentConfigurationApply()) {
                log.info("Applying native image agent generated files to current native executable build");
                nativeImageAgentConfigDirectoryProducer.produce(new NativeImageAgentConfigDirectoryBuildItem(targetDirName));
            }
        }
    }

    private static void logConfigurationFileContents(String configurationFileName, Path targetPath) throws IOException {
        try (Stream<String> lines = Files.lines(targetPath.resolve(configurationFileName))) {
            log.infof("Generated %s:%n%s", configurationFileName,
                    lines.collect(Collectors.joining(System.lineSeparator())));
        }
    }

    private void transformJsonObject(Path base, String name, Path target, JsonTransform transform) throws IOException {
        final String original = Files.readString(base.resolve(name));
        final JsonReader.JsonObject jsonRead = JsonReader.of(original).read();
        final Json.JsonObjectBuilder jsonBuilder = Json.object().skipEscape(true);
        jsonBuilder.transform(jsonRead, transform);

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(target.resolve(name).toFile(), StandardCharsets.UTF_8))) {
            jsonBuilder.appendTo(writer);
        }
    }

    private boolean discardResource(JsonReader.JsonValue value) {
        if (value instanceof JsonReader.JsonMember) {
            final JsonReader.JsonMember member = (JsonReader.JsonMember) value;
            if ("pattern".equals(member.attribute().value())) {
                final JsonReader.JsonString memberValue = (JsonReader.JsonString) member.value();
                return resourceSkipPattern.matcher(memberValue.value()).find();
            }
        }

        return false;
    }

    private static Pattern discardPattern(String... ignoredElements) {
        final String pattern = Arrays.stream(ignoredElements).collect(Collectors.joining("|", ".*(", ").*"));
        return Pattern.compile(pattern);
    }
}
