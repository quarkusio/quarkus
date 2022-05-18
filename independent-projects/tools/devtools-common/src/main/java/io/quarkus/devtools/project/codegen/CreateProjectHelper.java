package io.quarkus.devtools.project.codegen;

import static java.util.Objects.requireNonNull;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.paths.PathTree;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;
import org.eclipse.aether.artifact.DefaultArtifact;

public class CreateProjectHelper {

    // ordering is important here, so let's keep them ordered
    public static final SortedSet<Integer> JAVA_VERSIONS_LTS = new TreeSet<>(List.of(11, 17));
    private static final int DEFAULT_JAVA_VERSION = 11;
    private static final int MAX_LTS_SUPPORTED_BY_KOTLIN = 17;
    public static final String DETECT_JAVA_RUNTIME_VERSION = "<<detect java runtime version>>";
    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("(\\d+)(?:\\..*)?");

    public static final String DEFAULT_GROUP_ID = "org.acme";
    public static final String DEFAULT_ARTIFACT_ID = "code-with-quarkus";
    public static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";

    private CreateProjectHelper() {
    }

    /**
     * {@link CreateProjectHelper#completeCatalog}
     *
     * @param catalog original extension catalog
     * @param extensions extra extensions to add to the catalog
     * @param mvn Maven artifact resolver
     * @return complete extension catalog
     * @throws BootstrapMavenException in case of a failure to resolve extensions requested by the user
     */
    public static ExtensionCatalog completeCatalogWithCoords(ExtensionCatalog catalog, Collection<ArtifactCoords> extensions,
            MavenArtifactResolver mvn) {
        final List<String> exts = extensions.stream()
                .filter(e -> e.getVersion() != null)
                .map(ArtifactCoords::toString)
                .collect(Collectors.toList());
        return completeCatalog(catalog, exts, mvn);
    }

    /**
     * This method checks whether extensions to be added are specified using complete artifact coordinates,
     * in which case they are resolved and added to the catalog so that their codestarts are picked up by the code generator.
     *
     * @param catalog original extension catalog
     * @param extensions extra extensions to add to the catalog
     * @param mvn Maven artifact resolver
     * @return complete extension catalog
     * @throws BootstrapMavenException in case of a failure to resolve extensions requested by the user
     */
    public static ExtensionCatalog completeCatalog(ExtensionCatalog catalog, Collection<String> extensions,
            MavenArtifactResolver mvn) {
        ExtensionCatalog.Mutable mutableCatalog = null;
        for (String extArg : extensions) {
            if (isFullArtifactCoords(extArg)) {
                var coords = ArtifactCoords.fromString(extArg.trim());
                final Path extJar;
                try {
                    extJar = mvn.resolve(new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(),
                            coords.getClassifier(), coords.getType(), coords.getVersion())).getArtifact().getFile().toPath();
                } catch (BootstrapMavenException e) {
                    throw new RuntimeException("Failed to resolve extension " + coords, e);
                }
                final Extension ext = PathTree.ofDirectoryOrArchive(extJar)
                        .apply(BootstrapConstants.EXTENSION_METADATA_PATH, visit -> {
                            if (visit == null) {
                                return null;
                            }
                            try {
                                return Extension.fromFile(visit.getPath());
                            } catch (IOException e) {
                                throw new IllegalStateException(
                                        "Failed to parse Quarkus extension metadata " + visit.getPath());
                            }
                        });
                if (ext != null) {
                    if (mutableCatalog == null) {
                        mutableCatalog = catalog.mutable();
                    }
                    var i = mutableCatalog.getExtensions().iterator();
                    boolean add = true;
                    while (i.hasNext()) {
                        final ArtifactCoords catalogCoords = i.next().getArtifact();
                        if (catalogCoords.getKey().equals(ext.getArtifact().getKey())) {
                            if (catalogCoords.getVersion().equals(ext.getArtifact().getVersion())) {
                                add = false;
                            } else {
                                i.remove();
                            }
                            break;
                        }
                    }
                    if (add) {
                        mutableCatalog.addExtension(ext);
                    }
                }
            }
        }
        if (mutableCatalog != null) {
            catalog = mutableCatalog.build();
        }
        return catalog;
    }

    private static boolean isFullArtifactCoords(String s) {
        if (s == null) {
            return false;
        }
        var firstColon = s.indexOf(':');
        if (firstColon > 0) {
            var lastColon = s.lastIndexOf(':');
            return lastColon > 0 && firstColon != lastColon;
        }
        return false;
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

        Integer javaFeatureVersionTarget = null;

        if (javaTarget != null && !DETECT_JAVA_RUNTIME_VERSION.equals(javaTarget)) {
            // Probably too much as we should push only the feature version but let's be as thorough as we used to be
            Matcher matcher = JAVA_VERSION_PATTERN.matcher(javaTarget);
            if (matcher.matches()) {
                javaFeatureVersionTarget = Integer.valueOf(matcher.group(1));
            }
        }

        if (javaFeatureVersionTarget == null) {
            javaFeatureVersionTarget = Runtime.version().feature();
        }

        int bestJavaLtsVersion = determineBestJavaLtsVersion(javaFeatureVersionTarget);

        if (SourceType.KOTLIN.equals(values.get(ProjectGenerator.SOURCE_TYPE))
                && bestJavaLtsVersion > MAX_LTS_SUPPORTED_BY_KOTLIN) {
            bestJavaLtsVersion = MAX_LTS_SUPPORTED_BY_KOTLIN;
        }

        values.put(ProjectGenerator.JAVA_TARGET, String.valueOf(bestJavaLtsVersion));
    }

    public static int determineBestJavaLtsVersion() {
        return determineBestJavaLtsVersion(Runtime.version().feature());
    }

    public static int determineBestJavaLtsVersion(int runtimeVersion) {
        int bestLtsVersion = DEFAULT_JAVA_VERSION;
        for (int ltsVersion : JAVA_VERSIONS_LTS) {
            if (ltsVersion > runtimeVersion) {
                break;
            }
            bestLtsVersion = ltsVersion;
        }
        return bestLtsVersion;
    }

    public static Set<String> sanitizeExtensions(Set<String> extensions) {
        if (extensions == null) {
            return extensions = Set.of();
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
