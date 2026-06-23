package io.quarkus.maven.worker;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.JarResult;

public final class MavenAugmentWorkerConfig {

    public static final String MODE_BUILD = "build";
    public static final String MODE_CODEGEN = "codegen";

    private static final String KEY_MODE = "mode";
    private static final String KEY_APP_MODEL = "appModel";
    private static final String KEY_BUILD_PROPS = "buildProperties";
    private static final String KEY_PROJECT_ROOT = "projectRoot";
    private static final String KEY_BUILD_DIR = "buildDir";
    private static final String KEY_BASE_NAME = "baseName";
    private static final String KEY_ORIGINAL_BASE_NAME = "originalBaseName";
    private static final String KEY_RESULT = "result";
    private static final String KEY_GENERATED_SOURCES_DIR = "generatedSourcesDir";
    private static final String KEY_LAUNCH_MODE = "launchMode";
    private static final String KEY_TEST = "test";
    private static final String KEY_SOURCE_PARENTS = "sourceParents";

    private static final String RESULT_JAR_PATH = "jar.path";
    private static final String RESULT_JAR_ORIGINAL = "jar.originalArtifact";
    private static final String RESULT_JAR_LIBRARY_DIR = "jar.libraryDir";
    private static final String RESULT_JAR_CLASSIFIER = "jar.classifier";
    private static final String RESULT_JAR_MUTABLE = "jar.mutable";
    private static final String RESULT_JAR_UBER = "jar.uberJar";

    final String mode;
    final Path appModelPath;
    final Path buildPropertiesPath;
    final Path projectRoot;
    final Path buildDir;
    final String baseName;
    final String originalBaseName;
    final Path resultPath;
    final Path generatedSourcesDir;
    final String launchMode;
    final boolean test;
    final List<Path> sourceParents;

    private MavenAugmentWorkerConfig(Properties properties) {
        mode = properties.getProperty(KEY_MODE);
        appModelPath = Path.of(required(properties, KEY_APP_MODEL));
        buildPropertiesPath = Path.of(required(properties, KEY_BUILD_PROPS));
        projectRoot = Path.of(required(properties, KEY_PROJECT_ROOT));
        buildDir = Path.of(required(properties, KEY_BUILD_DIR));
        baseName = required(properties, KEY_BASE_NAME);
        originalBaseName = required(properties, KEY_ORIGINAL_BASE_NAME);
        resultPath = properties.containsKey(KEY_RESULT) ? Path.of(properties.getProperty(KEY_RESULT)) : null;
        generatedSourcesDir = properties.containsKey(KEY_GENERATED_SOURCES_DIR)
                ? Path.of(properties.getProperty(KEY_GENERATED_SOURCES_DIR))
                : null;
        launchMode = properties.getProperty(KEY_LAUNCH_MODE, "NORMAL");
        test = Boolean.parseBoolean(properties.getProperty(KEY_TEST, "false"));
        sourceParents = new ArrayList<>();
        String parents = properties.getProperty(KEY_SOURCE_PARENTS);
        if (parents != null && !parents.isBlank()) {
            for (String parent : parents.split(File.pathSeparator)) {
                sourceParents.add(Path.of(parent));
            }
        }
    }

    public static Path write(Path target,
            String mode,
            Path appModelPath,
            Path buildPropertiesPath,
            Path projectRoot,
            Path buildDir,
            String baseName,
            String originalBaseName,
            Path resultPath,
            Path generatedSourcesDir,
            String launchMode,
            boolean test,
            List<Path> sourceParents) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(KEY_MODE, mode);
        properties.setProperty(KEY_APP_MODEL, appModelPath.toString());
        properties.setProperty(KEY_BUILD_PROPS, buildPropertiesPath.toString());
        properties.setProperty(KEY_PROJECT_ROOT, projectRoot.toString());
        properties.setProperty(KEY_BUILD_DIR, buildDir.toString());
        properties.setProperty(KEY_BASE_NAME, baseName);
        properties.setProperty(KEY_ORIGINAL_BASE_NAME, originalBaseName);
        if (resultPath != null) {
            properties.setProperty(KEY_RESULT, resultPath.toString());
        }
        if (generatedSourcesDir != null) {
            properties.setProperty(KEY_GENERATED_SOURCES_DIR, generatedSourcesDir.toString());
        }
        properties.setProperty(KEY_LAUNCH_MODE, launchMode);
        properties.setProperty(KEY_TEST, Boolean.toString(test));
        if (sourceParents != null && !sourceParents.isEmpty()) {
            StringBuilder parents = new StringBuilder();
            for (Path parent : sourceParents) {
                if (parents.length() > 0) {
                    parents.append(File.pathSeparator);
                }
                parents.append(parent);
            }
            properties.setProperty(KEY_SOURCE_PARENTS, parents.toString());
        }
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            properties.store(writer, "Quarkus Maven augment worker configuration");
        }
        return target;
    }

    static MavenAugmentWorkerConfig read(Path configPath) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return new MavenAugmentWorkerConfig(properties);
    }

    static void writeBuildResult(AugmentResult result, Path resultPath) throws IOException {
        Properties properties = new Properties();
        JarResult jar = result.getJar();
        if (jar != null) {
            putIfNotNull(properties, RESULT_JAR_PATH, pathToString(jar.getPath()));
            putIfNotNull(properties, RESULT_JAR_ORIGINAL, pathToString(jar.getOriginalArtifact()));
            putIfNotNull(properties, RESULT_JAR_LIBRARY_DIR, pathToString(jar.getLibraryDir()));
            putIfNotNull(properties, RESULT_JAR_CLASSIFIER, jar.getClassifier());
            properties.setProperty(RESULT_JAR_MUTABLE, Boolean.toString(jar.mutable()));
            properties.setProperty(RESULT_JAR_UBER, Boolean.toString(jar.isUberJar()));
        }
        if (result.getGraalVMInfo() != null) {
            for (Map.Entry<String, String> entry : result.getGraalVMInfo().entrySet()) {
                properties.setProperty("graalvm." + entry.getKey(), entry.getValue());
            }
        }
        try (Writer writer = Files.newBufferedWriter(resultPath, StandardCharsets.UTF_8)) {
            properties.store(writer, "Quarkus Maven augment worker result");
        }
    }

    public static AugmentResult readBuildResult(Path resultPath) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(resultPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        JarResult jar = null;
        if (properties.containsKey(RESULT_JAR_PATH)) {
            Path path = Path.of(properties.getProperty(RESULT_JAR_PATH));
            Path originalArtifact = properties.containsKey(RESULT_JAR_ORIGINAL)
                    ? Path.of(properties.getProperty(RESULT_JAR_ORIGINAL))
                    : null;
            Path libraryDir = properties.containsKey(RESULT_JAR_LIBRARY_DIR)
                    ? Path.of(properties.getProperty(RESULT_JAR_LIBRARY_DIR))
                    : null;
            String classifier = properties.getProperty(RESULT_JAR_CLASSIFIER);
            boolean mutable = Boolean.parseBoolean(properties.getProperty(RESULT_JAR_MUTABLE, "false"));
            jar = new JarResult(path, originalArtifact, libraryDir, mutable, classifier);
        }
        Map<String, String> graalVMInfo = new HashMap<>();
        for (String name : properties.stringPropertyNames()) {
            if (name.startsWith("graalvm.")) {
                graalVMInfo.put(name.substring("graalvm.".length()), properties.getProperty(name));
            }
        }
        return new AugmentResult(List.of(), jar, null, graalVMInfo);
    }

    private static void putIfNotNull(Properties properties, String key, String value) {
        if (value != null) {
            properties.setProperty(key, value);
        }
    }

    private static String pathToString(Path path) {
        return path == null ? null : path.toString();
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required worker configuration property: " + key);
        }
        return value;
    }
}
