package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.jboss.logging.Logger;

import io.quarkus.builder.Json;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageJniConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImagePropertiesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageReflectConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceConfigBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.util.ArtifactResourceResolver;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathFilter;

/**
 * Parses META-INF/native-image configuration files from dependencies and produces build items
 * containing the parsed metadata. This allows Quarkus to process and validate native-image
 * configuration instead of just excluding it.
 */
public class NativeImageMetadataParserStep {

    private static final Logger log = Logger.getLogger(NativeImageMetadataParserStep.class);

    private static final String NATIVE_IMAGE_ROOT = "META-INF/native-image/";
    private static final PathFilter NATIVE_IMAGE_FILTER = PathFilter.forIncludes(List.of(NATIVE_IMAGE_ROOT + "**"));

    private static final String PROPERTIES_FILE = "native-image.properties";
    private static final String REFLECT_CONFIG_FILE = "reflect-config.json";
    private static final String RESOURCE_CONFIG_FILE = "resource-config.json";
    private static final String JNI_CONFIG_FILE = "jni-config.json";
    private static final String PROXY_CONFIG_FILE = "proxy-config.json";

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void parseNativeImageMetadata(CurateOutcomeBuildItem curateOutcome,
            BuildProducer<NativeImagePropertiesBuildItem> propertiesProducer,
            BuildProducer<NativeImageReflectConfigBuildItem> reflectConfigProducer,
            BuildProducer<NativeImageResourceConfigBuildItem> resourceConfigProducer,
            BuildProducer<NativeImageJniConfigBuildItem> jniConfigProducer,
            BuildProducer<NativeImageProxyConfigBuildItem> proxyConfigProducer) {

        Collection<ResolvedDependency> dependencies = curateOutcome.getApplicationModel().getDependencies();

        // Create a resolver for all dependencies
        ArtifactResourceResolver resolver = ArtifactResourceResolver.of(dependencies,
                dependencies.stream().map(ResolvedDependency::getKey).toList());

        // Get all native-image related resource paths
        Collection<Path> nativeImagePaths = resolver.resourcePathList(NATIVE_IMAGE_FILTER);

        for (Path resourcePath : nativeImagePaths) {
            String pathString = resourcePath.toString();

            // Find the artifact this resource belongs to
            ResolvedDependency sourceArtifact = findSourceArtifact(dependencies, resourcePath);
            if (sourceArtifact == null) {
                log.debugf("Could not determine source artifact for resource: %s", pathString);
                continue;
            }

            String jarFile = sourceArtifact.getKey().toString();
            String resourceName = pathString;

            try {
                if (pathString.endsWith(PROPERTIES_FILE)) {
                    parsePropertiesFile(sourceArtifact, resourcePath, jarFile, resourceName, propertiesProducer);
                } else if (pathString.endsWith(REFLECT_CONFIG_FILE)) {
                    parseJsonConfigFile(sourceArtifact, resourcePath, jarFile, resourceName, reflectConfigProducer,
                            NativeImageReflectConfigBuildItem::new);
                } else if (pathString.endsWith(RESOURCE_CONFIG_FILE)) {
                    parseJsonConfigFile(sourceArtifact, resourcePath, jarFile, resourceName, resourceConfigProducer,
                            NativeImageResourceConfigBuildItem::new);
                } else if (pathString.endsWith(JNI_CONFIG_FILE)) {
                    parseJsonConfigFile(sourceArtifact, resourcePath, jarFile, resourceName, jniConfigProducer,
                            NativeImageJniConfigBuildItem::new);
                } else if (pathString.endsWith(PROXY_CONFIG_FILE)) {
                    parseJsonConfigFile(sourceArtifact, resourcePath, jarFile, resourceName, proxyConfigProducer,
                            NativeImageProxyConfigBuildItem::new);
                }
            } catch (Exception e) {
                log.warnf(e, "Failed to parse native-image metadata file %s from %s", resourceName, jarFile);
            }
        }
    }

    private void parsePropertiesFile(ResolvedDependency sourceArtifact, Path resourcePath, String jarFile, String resourceName,
            BuildProducer<NativeImagePropertiesBuildItem> producer) throws IOException {

        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(sourceArtifact.getContentTree().getPath(resourcePath))) {
            props.load(is);
        }

        String argsValue = props.getProperty("Args");
        if (argsValue != null && !argsValue.trim().isEmpty()) {
            // Parse the Args value into individual arguments
            List<String> args = parseArgs(argsValue);
            producer.produce(new NativeImagePropertiesBuildItem(jarFile, resourceName, args));
            log.debugf("Parsed native-image.properties from %s: %s", jarFile, args);
        }
    }

    private <T> void parseJsonConfigFile(ResolvedDependency sourceArtifact, Path resourcePath, String jarFile, String resourceName,
            BuildProducer<T> producer, BuildItemFactory<T> factory) throws IOException {

        String content;
        try (InputStream is = Files.newInputStream(sourceArtifact.getContentTree().getPath(resourcePath))) {
            content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Basic validation - ensure it's valid JSON
        try {
            Json.parse(content);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON in " + resourceName + " from " + jarFile, e);
        }

        T buildItem = factory.create(jarFile, resourceName, content);
        producer.produce(buildItem);
        log.debugf("Parsed JSON config from %s: %s", jarFile, resourceName);
    }

    public static List<String> parseArgs(String argsValue) {
        // Simple argument parsing - split on spaces but preserve quoted strings
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';

        for (char c : argsValue.toCharArray()) {
            if (!inQuotes && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else if ((c == '"' || c == '\'') && (!inQuotes || c == quoteChar)) {
                inQuotes = !inQuotes;
                if (inQuotes) {
                    quoteChar = c;
                } else {
                    // Don't include closing quote in the argument
                    continue;
                }
                current.append(c); // Include opening quote in the argument
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }

    private ResolvedDependency findSourceArtifact(Collection<ResolvedDependency> dependencies, Path resourcePath) {
        // The resourcePath is relative to the artifact root, so we need to find which artifact contains it
        // This is a simplified approach - in practice, we might need to check the path structure
        for (ResolvedDependency dep : dependencies) {
            try {
                if (dep.getContentTree().contains(resourcePath.toString())) {
                    return dep;
                }
            } catch (Exception e) {
                // Ignore and continue
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface BuildItemFactory<T> {
        T create(String jarFile, String resourceName, String content);
    }
}
