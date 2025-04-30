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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.builder.Json;
import io.quarkus.builder.JsonReader;
import io.quarkus.builder.JsonTransform;
import io.quarkus.builder.json.JsonMember;
import io.quarkus.builder.json.JsonObject;
import io.quarkus.builder.json.JsonString;
import io.quarkus.builder.json.JsonValue;

/**
 * Post-processes native image agent generated configuration to trim any unnecessary configuration.
 */
@Mojo(name = "native-image-agent", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class NativeImageAgentMojo extends QuarkusBootstrapMojo {

    private final Pattern resourceSkipPattern;

    public NativeImageAgentMojo() {
        // Exclude resource configuration for resources that Quarkus takes care of registering.
        resourceSkipPattern = discardPattern("application.properties", "jakarta", "jboss",
                "logging.properties", "microprofile",
                "quarkus", "slf4j", "smallrye", "vertx");
    }

    @Override
    protected boolean beforeExecute() throws MojoExecutionException, MojoFailureException {
        // Only execute transformation if integration tests were run in JVM mode
        return !QuarkusBootstrapMojo.isNativeProfileEnabled(mavenProject());
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        final String dirName = "native-image-agent-base-config";
        final Path basePath = buildDir().toPath().resolve(Path.of(dirName));
        getLog().debug("Checking if native image agent config folder exits at " + basePath);
        if (basePath.toFile().exists()) {
            try {
                final Path targetPath = buildDir().toPath().resolve(Path.of("native-image-agent-final-config"));
                if (!targetPath.toFile().exists()) {
                    targetPath.toFile().mkdirs();
                }
                getLog().debug("Native image agent config folder exits, copy and transform to " + targetPath);
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
                        getLog().info("Discovered native image agent generated files in " + targetPath);
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

    private void transformJsonObject(Path base, String name, Path target, JsonTransform transform) throws IOException {
        getLog().debug("Discarding resources from native image configuration that match the following regular expression: "
                + resourceSkipPattern);
        final String original = Files.readString(base.resolve(name));
        final JsonObject jsonRead = JsonReader.of(original).read();
        final Json.JsonObjectBuilder jsonBuilder = Json.object(false, true);
        jsonBuilder.transform(jsonRead, transform);

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(target.resolve(name).toFile(), StandardCharsets.UTF_8))) {
            jsonBuilder.appendTo(writer);
        }
    }

    private boolean discardResource(JsonValue value) {
        if (value instanceof JsonMember) {
            final JsonMember member = (JsonMember) value;
            if ("pattern".equals(member.attribute().value())) {
                final JsonString memberValue = (JsonString) member.value();
                final boolean discarded = resourceSkipPattern.matcher(memberValue.value()).find();
                if (discarded) {
                    getLog().debug("Discarded included resource with pattern: " + memberValue.value());
                }
                return discarded;
            }
        }

        return false;
    }

    private static Pattern discardPattern(String... ignoredElements) {
        final String pattern = Arrays.stream(ignoredElements).collect(Collectors.joining("|", ".*(", ").*"));
        return Pattern.compile(pattern);
    }
}
