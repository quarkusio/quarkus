package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageJniConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImagePropertiesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageReflectConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceConfigBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
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

        // Scan all dependencies for native-image metadata
        for (ResolvedDependency dependency : dependencies) {
            try {
                scanDependencyForNativeImageMetadata(dependency, propertiesProducer, reflectConfigProducer,
                        resourceConfigProducer, jniConfigProducer, proxyConfigProducer);
            } catch (Exception e) {
                log.debugf(e, "Failed to scan dependency %s for native-image metadata", dependency.getKey());
            }
        }
    }

    private void scanDependencyForNativeImageMetadata(ResolvedDependency dependency,
            BuildProducer<NativeImagePropertiesBuildItem> propertiesProducer,
            BuildProducer<NativeImageReflectConfigBuildItem> reflectConfigProducer,
            BuildProducer<NativeImageResourceConfigBuildItem> resourceConfigProducer,
            BuildProducer<NativeImageJniConfigBuildItem> jniConfigProducer,
            BuildProducer<NativeImageProxyConfigBuildItem> proxyConfigProducer) throws IOException {

        try (var openTree = dependency.getContentTree().open()) {
            // Walk through all files in META-INF/native-image
            dependency.getContentTree().walk(visit -> {
                String path = visit.getPath().toString();
                if (path.startsWith(NATIVE_IMAGE_ROOT)) {
                    String relativePath = path.substring(NATIVE_IMAGE_ROOT.length());
                    if (!relativePath.isEmpty()) {
                        try {
                            processNativeImageFile(dependency, visit.getPath(), relativePath,
                                    propertiesProducer, reflectConfigProducer, resourceConfigProducer,
                                    jniConfigProducer, proxyConfigProducer);
                        } catch (Exception e) {
                            log.debugf(e, "Failed to process native-image file %s from %s", path, dependency.getKey());
                        }
                    }
                }
            });
        }
    }

    private void processNativeImageFile(ResolvedDependency dependency, Path filePath, String relativePath,
            BuildProducer<NativeImagePropertiesBuildItem> propertiesProducer,
            BuildProducer<NativeImageReflectConfigBuildItem> reflectConfigProducer,
            BuildProducer<NativeImageResourceConfigBuildItem> resourceConfigProducer,
            BuildProducer<NativeImageJniConfigBuildItem> jniConfigProducer,
            BuildProducer<NativeImageProxyConfigBuildItem> proxyConfigProducer) throws IOException {

        String jarFile = dependency.getKey().toString();
        String resourceName = NATIVE_IMAGE_ROOT + relativePath;

        if (relativePath.endsWith(PROPERTIES_FILE)) {
            parsePropertiesFile(dependency, filePath, jarFile, resourceName, propertiesProducer);
        } else if (relativePath.endsWith(REFLECT_CONFIG_FILE)) {
            String content = readAndValidateJsonFile(filePath, jarFile, resourceName);
            reflectConfigProducer.produce(new NativeImageReflectConfigBuildItem(jarFile, resourceName, content));
        } else if (relativePath.endsWith(RESOURCE_CONFIG_FILE)) {
            String content = readAndValidateJsonFile(filePath, jarFile, resourceName);
            resourceConfigProducer.produce(new NativeImageResourceConfigBuildItem(jarFile, resourceName, content));
        } else if (relativePath.endsWith(JNI_CONFIG_FILE)) {
            String content = readAndValidateJsonFile(filePath, jarFile, resourceName);
            jniConfigProducer.produce(new NativeImageJniConfigBuildItem(jarFile, resourceName, content));
        } else if (relativePath.endsWith(PROXY_CONFIG_FILE)) {
            String content = readAndValidateJsonFile(filePath, jarFile, resourceName);
            proxyConfigProducer.produce(new NativeImageProxyConfigBuildItem(jarFile, resourceName, content));
        }
    }

    private void parsePropertiesFile(ResolvedDependency sourceArtifact, Path actualPath, String jarFile, String resourceName,
            BuildProducer<NativeImagePropertiesBuildItem> producer) throws IOException {

        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(actualPath)) {
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

    private String readAndValidateJsonFile(Path actualPath, String jarFile, String resourceName) throws IOException {
        String content = Files.readString(actualPath, StandardCharsets.UTF_8);

        // Basic validation - ensure it's valid JSON-like content
        content = content.trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Empty content in " + resourceName + " from " + jarFile);
        }
        if (!((content.startsWith("{") && content.endsWith("}")) ||
                (content.startsWith("[") && content.endsWith("]")))) {
            throw new IllegalArgumentException(
                    "Content does not appear to be valid JSON in " + resourceName + " from " + jarFile);
        }

        log.debugf("Parsed JSON config from %s: %s", jarFile, resourceName);
        return content;
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
                }
                current.append(c); // Include quotes in the argument
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }

}
