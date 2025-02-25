package io.quarkus.devtools.project.update.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.eclipse.aether.artifact.Artifact;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.DependencyUtils;
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
        Map<String, String> recipes = new LinkedHashMap<>();
        String propRewritePluginVersion = null;

        for (String gav : gavs) {

            Map<String, String[]> recipeDirectoryNames = new LinkedHashMap<>();
            recipeDirectoryNames.put("core", new String[] { currentVersion, targetVersion });
            for (ExtensionUpdateInfo dep : topExtensionDependency) {
                recipeDirectoryNames.put(
                        toKey(dep),
                        new String[] { dep.getCurrentDep().getVersion(), dep.getRecommendedDependency().getVersion() });
            }

            try {
                final Artifact artifact = artifactResolver.resolve(DependencyUtils.toArtifact(gav)).getArtifact();
                String resolvedGAV = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
                artifacts.add(resolvedGAV);
                final ResourceLoader resourceLoader = ResourceLoaders.resolveFileResourceLoader(
                        artifact.getFile());
                Map<String, String> newRecipes = fetchUpdateRecipes(resourceLoader, "quarkus-updates", recipeDirectoryNames);
                recipes.putAll(newRecipes);
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

                log.info(String.format(
                        "Resolved %s with %s recipe(s) to update from %s to %s (initially made for OpenRewrite %s plugin version: %s) ",
                        gav,
                        recipes.size(),
                        currentVersion,
                        targetVersion,
                        buildTool,
                        propRewritePluginVersion));

            } catch (BootstrapMavenException e) {
                throw new RuntimeException("Failed to resolve artifact: " + gav, e);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load recipes in artifact: " + gav, e);
            }
        }

        return new FetchResult(String.join(",", artifacts),
                new ArrayList<>(recipes.values()), propRewritePluginVersion);
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
        final DefaultArtifactVersion recipeAVersion = new DefaultArtifactVersion(recipeVersion);
        final DefaultArtifactVersion currentAVersion = new DefaultArtifactVersion(currentVersion);
        final DefaultArtifactVersion targetAVersion = new DefaultArtifactVersion(targetVersion);
        return currentAVersion.compareTo(recipeAVersion) < 0 && targetAVersion.compareTo(recipeAVersion) >= 0;
    }

    static Map<String, String> fetchUpdateRecipes(ResourceLoader resourceLoader, String location,
            Map<String, String[]> recipeDirectoryNames) throws IOException {
        return resourceLoader.loadResourceAsPath(location,
                path -> {
                    try (final Stream<Path> pathStream = Files.walk(path)) {
                        return pathStream
                                .filter(Files::isDirectory)
                                .flatMap(dir -> applyStartsWith(toKey(path, dir), recipeDirectoryNames).stream()
                                        .flatMap(key -> {
                                            String versions[] = recipeDirectoryNames.get(key);
                                            if (versions != null && versions.length != 0) {
                                                try {
                                                    Stream<Path> recipePath = Files.walk(dir);
                                                    return recipePath
                                                            .filter(p -> p.getFileName().toString()
                                                                    .matches("^\\d\\H+.ya?ml$"))
                                                            .filter(p -> shouldApplyRecipe(p.getFileName().toString(),
                                                                    versions[0], versions[1]))
                                                            .sorted(RecipeVersionComparator.INSTANCE)
                                                            .map(p -> {
                                                                try {
                                                                    return new String[] { p.toString(),
                                                                            new String(Files.readAllBytes(p)) };
                                                                } catch (IOException e) {
                                                                    throw new RuntimeException("Error reading file: " + p,
                                                                            e);
                                                                }
                                                            })
                                                            .onClose(() -> recipePath.close());
                                                } catch (IOException e) {
                                                    throw new RuntimeException("Error traversing directory: " + dir, e);
                                                }
                                            }
                                            return null;
                                        }))
                                .filter(Objects::nonNull)
                                //results are collected to the map, because there could be duplicated matches in case of wildcard matching
                                .collect(Collectors.toMap(
                                        sa -> sa[0],
                                        sa -> sa[1],
                                        (v1, v2) -> {
                                            //Recipe with the same path already loaded. This can happen because of wildcards
                                            //in case the content differs (which can not happen in the current impl),
                                            //content is amended
                                            if (!v1.equals(v2)) {
                                                return v1 + "\n" + v2;
                                            }
                                            return v1;
                                        },
                                        LinkedHashMap::new));
                    } catch (IOException e) {
                        throw new RuntimeException("Error traversing base directory", e);
                    }
                });

    }

    private static String toKey(ExtensionUpdateInfo dep) {
        return String.format("%s:%s", dep.getCurrentDep().getArtifact().getGroupId(),
                dep.getCurrentDep().getArtifact().getArtifactId());
    }

    static String toKey(Path parentDir, Path recipeDir) {
        var _path = parentDir.relativize(recipeDir).toString();
        return _path
                .replaceAll("(^[/\\\\])|([/\\\\]$)", "")
                .replaceAll("[/\\\\]", ":");
    }

    static List<String> applyStartsWith(String key, Map<String, String[]> recipeDirectoryNames) {
        //list for all keys, that matches dir (could be more items in case of wildcard at the end
        List<String> matchedRecipeKeys;
        //Current implementation detects whether key starts with an existing recipe folder
        return recipeDirectoryNames.keySet().stream()
                .filter(k -> k.startsWith(key))
                .collect(Collectors.toList());
    }

    private static class RecipeVersionComparator implements Comparator<Path> {

        private static final RecipeVersionComparator INSTANCE = new RecipeVersionComparator();

        @Override
        public int compare(Path recipePath1, Path recipePath2) {
            DefaultArtifactVersion recipeVersion1 = new DefaultArtifactVersion(
                    VERSION_EXTRACTION_PATTERN.matcher(recipePath1.getFileName().toString()).replaceFirst(""));
            DefaultArtifactVersion recipeVersion2 = new DefaultArtifactVersion(
                    VERSION_EXTRACTION_PATTERN.matcher(recipePath2.getFileName().toString()).replaceFirst(""));

            return recipeVersion1.compareTo(recipeVersion2);
        }
    }
}
