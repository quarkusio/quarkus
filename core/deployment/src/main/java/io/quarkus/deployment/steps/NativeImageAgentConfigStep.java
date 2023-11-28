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

import io.quarkus.builder.Json;
import io.quarkus.builder.JsonReader;
import io.quarkus.builder.JsonTransform;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageAgentConfigDirectoryBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

public class NativeImageAgentConfigStep {
    private final Pattern jniSkipPattern;
    private final Pattern proxySkipPattern;
    private final Pattern reflectionSkipPattern;
    private final Pattern resourceSkipPattern;
    private final Pattern serializationSkipPattern;

    public NativeImageAgentConfigStep() {
        jniSkipPattern = discardPattern("apache.maven");
        proxySkipPattern = discardPattern("apache.http");
        reflectionSkipPattern = discardPattern("apache.http", "apache.commons", "apache.maven", "aether", "hamcrest", "jackson",
                "jakarta", "jboss", "junit",
                "groovy", "gson", "netty", "quarkus", "restassured", "smallrye", "Test", "vertx");
        resourceSkipPattern = discardPattern("apache.maven", "application.properties", "groovy", "jboss", "junit",
                "logging.properties", "microprofile", "quarkus", "slf4j", "smallrye", "surefire", "Test.class");
        serializationSkipPattern = discardPattern("quarkus", "junit");
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void transformConfig(BuildProducer<NativeImageAgentConfigDirectoryBuildItem> nativeImageAgentConfigDirectoryProducer,
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
            nativeImageAgentConfigDirectoryProducer.produce(new NativeImageAgentConfigDirectoryBuildItem(targetDirName));

            transformJsonArray(basePath, "jni-config.json", targetPath,
                    JsonTransform.dropping(v -> discardNamed(v, jniSkipPattern)));
            transformJsonArray(basePath, "proxy-config.json", targetPath, JsonTransform.dropping(this::discardProxyInterface));
            transformJsonArray(basePath, "reflect-config.json", targetPath,
                    JsonTransform.dropping(v -> discardNamed(v, reflectionSkipPattern)));
            transformJsonObject(basePath, "resource-config.json", targetPath, JsonTransform.dropping(this::discardPattern));
            transformJsonObject(basePath, "serialization-config.json", targetPath,
                    JsonTransform.dropping(v -> discardNamed(v, serializationSkipPattern)));
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

    private void transformJsonArray(Path base, String name, Path target, JsonTransform transform) throws IOException {
        final String original = Files.readString(base.resolve(name));
        final JsonReader.JsonArray jsonRead = JsonReader.of(original).read();
        final Json.JsonArrayBuilder jsonBuilder = Json.array().skipEscape(true);
        jsonBuilder.transform(jsonRead, transform);

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(target.resolve(name).toFile(), StandardCharsets.UTF_8))) {
            jsonBuilder.appendTo(writer);
        }
    }

    private boolean discardProxyInterface(JsonReader.JsonValue value) {
        if (value instanceof JsonReader.JsonObject) {
            final JsonReader.JsonObject obj = (JsonReader.JsonObject) value;
            return obj.<JsonReader.JsonArray> get("interfaces").<JsonReader.JsonString> stream()
                    .anyMatch(proxyInterface -> proxySkipPattern.matcher(proxyInterface.value()).find());
        }

        return false;
    }

    private boolean discardPattern(JsonReader.JsonValue value) {
        if (value instanceof JsonReader.JsonMember) {
            final JsonReader.JsonMember member = (JsonReader.JsonMember) value;
            if ("pattern".equals(member.attribute().value())) {
                final JsonReader.JsonString memberValue = (JsonReader.JsonString) member.value();
                return resourceSkipPattern.matcher(memberValue.value()).find();
            }
        }

        return false;
    }

    private boolean discardNamed(JsonReader.JsonValue value, Pattern pattern) {
        if (value instanceof JsonReader.JsonObject) {
            final JsonReader.JsonObject obj = (JsonReader.JsonObject) value;
            final String name = obj.<JsonReader.JsonString> get("name").value();
            return pattern.matcher(name).find();
        }

        return false;
    }

    private static Pattern discardPattern(String... ignoredElements) {
        final String pattern = Arrays.stream(ignoredElements).collect(Collectors.joining("|", ".*(", ").*"));
        return Pattern.compile(pattern);
    }
}
