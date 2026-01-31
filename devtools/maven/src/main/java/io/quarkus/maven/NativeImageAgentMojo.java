package io.quarkus.maven;

import java.io.BufferedWriter;
import java.io.IOException;
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

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.Json.JsonObjectBuilder;
import io.quarkus.bootstrap.json.JsonArray;
import io.quarkus.bootstrap.json.JsonMember;
import io.quarkus.bootstrap.json.JsonObject;
import io.quarkus.bootstrap.json.JsonReader;
import io.quarkus.bootstrap.json.JsonString;
import io.quarkus.bootstrap.json.JsonTransform;
import io.quarkus.bootstrap.json.JsonValue;

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
                final Path reachabilityMetadataJsonPath = basePath.resolve("reachability-metadata.json");
                if (Files.exists(reachabilityMetadataJsonPath)) {
                    // GraalVM/Mandrel 25+
                    transformReachabilityMetadataJson(basePath, "reachability-metadata.json", targetPath);

                    if (getLog().isInfoEnabled()) {
                        getLog().info("Discovered native image agent generated files in " + targetPath);
                    }
                } else if (Files.exists(reflectConfigJsonPath)) {
                    Files.copy(reflectConfigJsonPath, targetPath.resolve("reflect-config.json"),
                            StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(basePath.resolve("serialization-config.json"), targetPath.resolve("serialization-config.json"),
                            StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(basePath.resolve("jni-config.json"), targetPath.resolve("jni-config.json"),
                            StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(basePath.resolve("proxy-config.json"), targetPath.resolve("proxy-config.json"),
                            StandardCopyOption.REPLACE_EXISTING);
                    transformJsonObject(basePath, "resource-config.json", targetPath,
                            JsonTransform.dropping(v -> discardResource("pattern", v)));

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

    private void transformReachabilityMetadataJson(Path base, String name, Path target) throws IOException {
        getLog().debug("Discarding resources from native image configuration that match the following regular expression: "
                + resourceSkipPattern);
        final String original = Files.readString(base.resolve(name));
        final JsonObject jsonRead = JsonReader.of(original).read();
        JsonArray resources = jsonRead.get("resources");
        if (resources != null) {
            final JsonObjectBuilder jsonBuilder = Json.object();
            jsonBuilder.transform(jsonRead, JsonTransform.dropping(v -> discardResource("glob", v)));

            try (BufferedWriter writer = Files.newBufferedWriter(target.resolve(name))) {
                jsonBuilder.appendTo(writer);
            }
        } else {
            Files.copy(base.resolve(name), target.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void transformJsonObject(Path base, String name, Path target, JsonTransform transform) throws IOException {
        getLog().debug("Discarding resources from native image configuration that match the following regular expression: "
                + resourceSkipPattern);
        final String original = Files.readString(base.resolve(name));
        final JsonObject jsonRead = JsonReader.of(original).read();
        final Json.JsonObjectBuilder jsonBuilder = Json.object();
        jsonBuilder.transform(jsonRead, transform);

        try (BufferedWriter writer = Files.newBufferedWriter(target.resolve(name))) {
            jsonBuilder.appendTo(writer);
        }
    }

    private boolean discardResource(String attribute, JsonValue value) {
        if (value instanceof JsonMember) {
            final JsonMember member = (JsonMember) value;
            if (attribute.equals(member.attribute().value())) {
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
