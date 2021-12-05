package io.quarkus.devtools.project.codegen;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;

public class CreateProjectHelper {

    private static final Set<String> JAVA_VERSIONS_LTS = Set.of("11", "17");
    private static final String DEFAULT_JAVA_VERSION = "11";
    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("(?:1\\.)?(\\d+)(?:\\..*)?");

    public static final String DEFAULT_GROUP_ID = "org.acme";
    public static final String DEFAULT_ARTIFACT_ID = "code-with-quarkus";
    public static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";

    private CreateProjectHelper() {
    }

    public static String checkClassName(String name) {
        if (!SourceVersion.isName(name)) { // checks for valid identifiers & use of keywords
            throw new IllegalArgumentException(name + " is not a valid class name");
        }
        return name;
    }

    public static String checkPackageName(String name) {
        if (!SourceVersion.isName(name)) { // checks for valid identifiers & use of keywords
            throw new IllegalArgumentException(name + " is not a valid package name");
        }
        return name;
    }

    public static Path checkProjectRootPath(Path outputPath, String name) {
        requireNonNull(name, "Must specify project name");
        requireNonNull(outputPath, "Must specify output path");

        Path projectRootPath = outputPath.resolve(name);
        if (projectRootPath.toFile().exists()) {
            throw new IllegalArgumentException(
                    "Target directory already exists: " + projectRootPath.toAbsolutePath().toString());
        }
        return projectRootPath;
    }

    public static Path createOutputDirectory(String targetDirectory) {
        Path origin = new File(System.getProperty("user.dir")).toPath();
        Path outputPath = (targetDirectory == null ? origin : origin.resolve(targetDirectory));
        try {
            Files.createDirectories(outputPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not create directory " + targetDirectory, e);
        }
        return outputPath;
    }

    public static SourceType determineSourceType(Set<String> extensions) {
        Optional<SourceType> sourceType = extensions.stream()
                .map(SourceType::parse)
                .filter(Optional::isPresent)
                .map(e -> e.orElse(SourceType.JAVA))
                .findAny();
        return sourceType.orElse(SourceType.JAVA);
    }

    public static void setJavaVersion(Map<String, Object> values, String javaTarget) {
        requireNonNull(values, "Must provide values");

        Matcher matcher = JAVA_VERSION_PATTERN
                .matcher(javaTarget != null ? javaTarget : System.getProperty("java.version", ""));

        if (matcher.matches()) {
            String versionExtracted = matcher.group(1);
            String version = JAVA_VERSIONS_LTS.contains(versionExtracted) ? versionExtracted : DEFAULT_JAVA_VERSION;
            values.put(ProjectGenerator.JAVA_TARGET, version);
        } else {
            values.put(ProjectGenerator.JAVA_TARGET, DEFAULT_JAVA_VERSION);
        }
    }

    public static Set<String> sanitizeExtensions(Set<String> extensions) {
        if (extensions == null) {
            return extensions = new HashSet<>();
        }
        return extensions.stream().filter(Objects::nonNull).map(String::trim).collect(Collectors.toSet());
    }

    public static void addSourceTypeExtensions(Set<String> extensions, SourceType sourceType) {
        if (sourceType == SourceType.KOTLIN) {
            extensions.add("quarkus-kotlin");
        } else if (sourceType == SourceType.SCALA) {
            extensions.add("quarkus-scala");
        }
    }

    public static void handleSpringConfiguration(Map<String, Object> values) {
        @SuppressWarnings("unchecked")
        Set<String> extensions = (Set<String>) values.get(ProjectGenerator.EXTENSIONS);

        handleSpringConfiguration(values, extensions);
    }

    public static void handleSpringConfiguration(Map<String, Object> values, Set<String> extensions) {
        requireNonNull(values, "Must provide values");
        requireNonNull(extensions, "Must provide extensions");

        if (containsSpringWeb(extensions)) {
            values.put(ProjectGenerator.IS_SPRING, true);
            if (containsRESTEasy(extensions)) {
                values.remove(ProjectGenerator.CLASS_NAME);
                values.remove(ProjectGenerator.RESOURCE_PATH);
            }
        }
    }

    private static boolean containsSpringWeb(Collection<String> extensions) {
        return extensions.stream().anyMatch(e -> e.toLowerCase().contains("spring-web"));
    }

    private static boolean containsRESTEasy(Collection<String> extensions) {
        return extensions.isEmpty() || extensions.stream().anyMatch(e -> e.toLowerCase().contains("resteasy"));
    }
}
