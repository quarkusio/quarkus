package io.quarkus.maven;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.builder.Json;
import io.quarkus.builder.JsonReader;
import io.quarkus.builder.JsonTransform;

@Mojo(name = "transform-native-image-agent-config", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class TransformNativeImageAgentConfigMojo extends QuarkusBootstrapMojo {

    private final Pattern resourceSkipPattern;

    public TransformNativeImageAgentConfigMojo() {
        resourceSkipPattern = discardPattern("apache.maven", "application.properties", "groovy",
                "java[.\\/](io|lang|net|nio|time|util)",
                "javax[.\\/](inject|xml)", "jdk\\/internal",
                "jboss",
                "junit", "logging.properties", "microprofile", "osgi\\/annotation", "plexus", "quarkus", "slf4j", "smallrye",
                "surefire",
                "Test.class");
    }

    @Override
    protected boolean beforeExecute() throws MojoExecutionException, MojoFailureException {
        return true;
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        final String dirName = "native-image-agent-base-config";
        final Path basePath = buildDir().toPath().resolve(Path.of(dirName));
        if (basePath.toFile().exists()) {
            try {
                final Path targetPath = buildDir().toPath().resolve(Path.of("native-image-agent-transformed-config"));
                if (!targetPath.toFile().exists()) {
                    targetPath.toFile().mkdirs();
                }
                final Path reflectConfigJsonPath = basePath.resolve("reflect-config.json");
                if (reflectConfigJsonPath.toFile().exists()) {
                    Files.copy(reflectConfigJsonPath, targetPath.resolve("reflect-config.json"),
                            StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(basePath.resolve("serialization-config.json"), targetPath.resolve("serialization-config.json"),
                            StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(basePath.resolve("jni-config.json"), targetPath.resolve("jni-config.json"),
                            StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(basePath.resolve("proxy-config.json"), targetPath.resolve("proxy-config.json"),
                            StandardCopyOption.REPLACE_EXISTING);
                    transformJsonObject(basePath, "resource-config.json", targetPath,
                            JsonTransform.dropping(this::discardResource));

                    if (getLog().isInfoEnabled()) {
                        getLog().info("Discovered native image agent generated files");
                        logConfigurationFileContents("reflect-config.json", targetPath);
                        logConfigurationFileContents("serialization-config.json", targetPath);
                        logConfigurationFileContents("jni-config.json", targetPath);
                        logConfigurationFileContents("proxy-config.json", targetPath);
                        logConfigurationFileContents("resource-config.json", targetPath);
                    }
                } else {
                    final Path reflectOriginsTxtPath = basePath.resolve("reflect-origins.txt");
                    if (reflectOriginsTxtPath.toFile().exists()) {
                        getLog().info("Native image agent configuration origin files exist, inspect them manually inside "
                                + basePath);
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to transform native image agent configuration", e);
            }
        } else {
            getLog().info("Missing " + dirName + " directory with native image agent configuration to transform");
        }
    }

    private void logConfigurationFileContents(String configurationFileName, Path targetPath) throws IOException {
        try (Stream<String> lines = Files.lines(targetPath.resolve(configurationFileName))) {
            getLog().info("Generated " + configurationFileName + ":" + System.lineSeparator() +
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
            jsonBuilder.appendTo(writer, true);
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
