package io.quarkus.devtools.project.update.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.aether.artifact.Artifact;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.DependencyUtils;
import io.quarkus.devtools.messagewriter.MessageFormatter;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.ExtensionUpdateInfo;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.platform.descriptor.loader.json.ResourceLoaders;

public final class QuarkusUpdatesRepository {

    private QuarkusUpdatesRepository() {
    }

    private static final String QUARKUS_UPDATE_RECIPES_GA = "io.quarkus:quarkus-update-recipes";
    private static final Pattern VERSION_EXTRACTION_PATTERN = Pattern.compile("[.][^.]+$");

    public static final String DEFAULT_UPDATE_RECIPES_VERSION = "LATEST";

    public static final String DEFAULT_MAVEN_REWRITE_PLUGIN_VERSION = "4.46.0";
    public static final String DEFAULT_GRADLE_REWRITE_PLUGIN_VERSION = "5.38.0";
    public static final String PROP_REWRITE_MAVEN_PLUGIN_VERSION = "rewrite-maven-plugin-version";
    public static final String PROP_REWRITE_GRADLE_PLUGIN_VERSION = "rewrite-gradle-plugin-version";

    public static FetchResult fetchRecipes(MessageWriter log, MavenArtifactResolver artifactResolver,
            BuildTool buildTool, String quarkusUpdateRecipes, String additionalUpdateRecipes, String currentVersion,
            String targetVersion, List<ExtensionUpdateInfo> topExtensionDependency) {

        List<String> gavs = new ArrayList<>();

        gavs.add(quarkusUpdateRecipes.contains(":") ? quarkusUpdateRecipes
                : QUARKUS_UPDATE_RECIPES_GA + ":" + quarkusUpdateRecipes);

        if (additionalUpdateRecipes != null) {
            gavs.addAll(Arrays.stream(additionalUpdateRecipes.split(",")).map(String::strip).toList());
        }

        List<String> artifacts = new ArrayList<>();
        List<String> recipes = new ArrayList<>();
        String propRewritePluginVersion = null;
        log.info("");

        for (String gav : gavs) {
            log.info("Resolving recipes from '%s':", gav.replace(":LATEST", ""));
            Map<String, VersionUpdate> recipeDirectoryNames = new LinkedHashMap<>();
            recipeDirectoryNames.put("core", new VersionUpdate(currentVersion, targetVersion));
            for (ExtensionUpdateInfo dep : topExtensionDependency) {
                recipeDirectoryNames.put(
                        toKey(dep),
                        new VersionUpdate(dep.getCurrentDep().getVersion(), dep.getRecommendedDependency().getVersion()));
            }

            try {
                final Artifact artifact = artifactResolver.resolve(DependencyUtils.toArtifact(gav)).getArtifact();
                String resolvedGAV = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
                artifacts.add(resolvedGAV);
                final ResourceLoader resourceLoader = ResourceLoaders.resolveFileResourceLoader(
                        artifact.getFile());
                List<String> newRecipes = fetchUpdateRecipes(log, resourceLoader, "quarkus-updates",
                        recipeDirectoryNames);
                recipes.addAll(newRecipes);
                final Properties props = resourceLoader.loadResourceAsPath("quarkus-updates/", p -> {
                    final Properties properties = new Properties();
                    final Path propPath = p.resolve("recipes.properties");
                    if (Files.isRegularFile(propPath)) {
                        try (final InputStream inStream = Files.newInputStream(propPath)) {
                            properties.load(inStream);
                        }
                    }
                    return properties;
                });

                final String pluginVersion = getPropRewritePluginVersion(props, buildTool);
                if (propRewritePluginVersion == null) {
                    propRewritePluginVersion = pluginVersion;
                } else if (!propRewritePluginVersion.equals(pluginVersion)) {
                    throw new RuntimeException(
                            "quarkus update artifacts require multiple rewrite plugin versions: " + propRewritePluginVersion
                                    + " and " + pluginVersion);
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "=> %d specific recipe" + (recipes.size() != 1 ? "s" : "")
                                    + " found (compatible with OpenRewrite %s plugin version: %s)",
                            recipes.size(),
                            buildTool,
                            propRewritePluginVersion));
                } else {
                    log.info(String.format(
                            "=> %s specific recipe" + (recipes.size() != 1 ? "s" : "") + " found",
                            MessageFormatter.green(String.valueOf(recipes.size()))));
                }

            } catch (BootstrapMavenException e) {
                throw new RuntimeException("Failed to resolve artifact: " + gav, e);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load recipes in artifact: " + gav, e);
            }
        }

        return new FetchResult(String.join(",", artifacts),
                recipes, propRewritePluginVersion);
    }

    private static String getPropRewritePluginVersion(Properties props, BuildTool buildTool) {
        switch (buildTool) {
            case MAVEN:
                return Optional.ofNullable(props.getProperty(PROP_REWRITE_MAVEN_PLUGIN_VERSION))
                        .orElse(DEFAULT_MAVEN_REWRITE_PLUGIN_VERSION);
            case GRADLE:
            case GRADLE_KOTLIN_DSL:
                return Optional.ofNullable(props.getProperty(PROP_REWRITE_GRADLE_PLUGIN_VERSION))
                        .orElse(DEFAULT_GRADLE_REWRITE_PLUGIN_VERSION);
            default:
                throw new IllegalStateException("This build tool does not support update " + buildTool);
        }
    }

    private static boolean isRecipeFile(Path p) {
        return p.getFileName().toString()
                .matches("^\\d\\H+.ya?ml$");
    }

    public static class FetchResult {

        private final String recipesGAV;
        private final List<String> recipes;
        private final String rewritePluginVersion;

        public FetchResult(String recipesGAV, List<String> recipes, String rewritePluginVersion) {
            this.recipesGAV = recipesGAV;
            this.rewritePluginVersion = rewritePluginVersion;
            this.recipes = recipes;
        }

        public String getRecipesGAV() {
            return recipesGAV;
        }

        public List<String> getRecipes() {
            return recipes;
        }

        public String getRewritePluginVersion() {
            return rewritePluginVersion;
        }
    }

    static boolean shouldApplyRecipe(String recipeFileName, String currentVersion, String targetVersion) {
        String recipeVersion = VERSION_EXTRACTION_PATTERN.matcher(recipeFileName).replaceFirst("");
        final ComparableVersion recipeAVersion = new ComparableVersion(recipeVersion);
        final ComparableVersion currentAVersion = new ComparableVersion(currentVersion);
        final ComparableVersion targetAVersion = new ComparableVersion(targetVersion);
        return currentAVersion.compareTo(recipeAVersion) < 0 && targetAVersion.compareTo(recipeAVersion) >= 0;
    }

    static boolean shouldApplyRecipe(String recipeFileName, Set<VersionUpdate> versions) {
        return versions.stream().anyMatch(version -> shouldApplyRecipe(recipeFileName, version.from(), version.to()));
    }

    static List<String> fetchUpdateRecipes(MessageWriter log, ResourceLoader resourceLoader, String location,
            Map<String, VersionUpdate> recipeDirectoryNames) throws IOException {
        return resourceLoader.loadResourceAsPath(location,
                path -> findRecipeDirectories(path, recipeDirectoryNames)
                        .flatMap(d -> {
                            log.info("* matching recipes directory '%s' found:", d.relativeDir);
                            Set<VersionUpdate> versions = d.versions();
                            try (Stream<Path> recipePath = Files.list(path.resolve(d.relativeDir()))) {
                                final List<String> recipes = recipePath
                                        .filter(QuarkusUpdatesRepository::isRecipeFile)
                                        .filter(p -> shouldApplyRecipe(p.getFileName().toString(),
                                                versions))
                                        .sorted(RecipeVersionComparator.INSTANCE)
                                        .map(p -> {
                                            try {
                                                log.info("    - '%s' (%s)",
                                                        p.getFileName().toString(),
                                                        versions.stream().map(v -> v.from() + " -> " + v.to())
                                                                .collect(Collectors.joining(", ")));
                                                return new String(Files.readAllBytes(p));
                                            } catch (IOException e) {
                                                throw new RuntimeException("Error reading file: " + p,
                                                        e);
                                            }
                                        }).toList();
                                if (recipes.isEmpty()) {
                                    log.info("\t\t- no matching recipes.");
                                }
                                return recipes.stream();
                            } catch (IOException e) {
                                throw new RuntimeException("Error listing files in directory: " + d.relativeDir(), e);
                            }
                        }).toList());
    }

    record RecipeDirectory(String relativeDir, Set<VersionUpdate> versions) {
    }

    record VersionUpdate(String from, String to) {
    }

    private static Stream<RecipeDirectory> findRecipeDirectories(Path rootDir,
            Map<String, VersionUpdate> recipeDirectoryNames) {
        return findDirsWithRecipes(rootDir).stream()
                .map(d -> {
                    String relativeDir = rootDir.relativize(d).toString();
                    return resolveVersionsForRecipesDir(relativeDir, toKey(relativeDir), recipeDirectoryNames);
                })
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public static Set<Path> findDirsWithRecipes(Path startDir) {
        try (var stream = Files.walk(startDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(QuarkusUpdatesRepository::isRecipeFile)
                    .map(Path::getParent)
                    .collect(Collectors.toSet()); // Ensures no duplicates
        } catch (IOException e) {
            throw new UncheckedIOException("Error while searching for recipe directories in " + startDir, e);
        }
    }

    private static String toKey(ExtensionUpdateInfo dep) {
        return String.format("%s:%s", dep.getCurrentDep().getArtifact().getGroupId(),
                dep.getCurrentDep().getArtifact().getArtifactId());
    }

    static String toKey(String relativeDir) {
        return relativeDir
                .replaceAll("(^[/\\\\])|([/\\\\]$)", "")
                .replaceAll("[/\\\\]", ":");
    }

    static Optional<RecipeDirectory> resolveVersionsForRecipesDir(String dir, String key,
            Map<String, VersionUpdate> recipeDirectoryNames) {
        final Set<VersionUpdate> matches = recipeDirectoryNames.entrySet().stream()
                .filter(e -> e.getKey().startsWith(key))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RecipeDirectory(dir, matches));
    }

    private static class RecipeVersionComparator implements Comparator<Path> {

        private static final RecipeVersionComparator INSTANCE = new RecipeVersionComparator();

        @Override
        public int compare(Path recipePath1, Path recipePath2) {
            ComparableVersion recipeVersion1 = new ComparableVersion(
                    VERSION_EXTRACTION_PATTERN.matcher(recipePath1.getFileName().toString()).replaceFirst(""));
            ComparableVersion recipeVersion2 = new ComparableVersion(
                    VERSION_EXTRACTION_PATTERN.matcher(recipePath2.getFileName().toString()).replaceFirst(""));

            return recipeVersion1.compareTo(recipeVersion2);
        }
    }
}
