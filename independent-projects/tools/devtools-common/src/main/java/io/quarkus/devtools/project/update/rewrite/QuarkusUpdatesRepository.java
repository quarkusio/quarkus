package io.quarkus.devtools.project.update.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.eclipse.aether.artifact.Artifact;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.DependencyUtils;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.platform.descriptor.loader.json.ResourceLoaders;

public final class QuarkusUpdatesRepository {

    private QuarkusUpdatesRepository() {
    }

    private static final String QUARKUS_RECIPE_GA = "io.quarkus:quarkus-update-recipes";
    public static final String DEFAULT_UPDATE_RECIPES_VERSION = "LATEST";

    public static final String DEFAULT_MAVEN_REWRITE_PLUGIN_VERSION = "4.46.0";
    public static final String DEFAULT_GRADLE_REWRITE_PLUGIN_VERSION = "5.38.0";
    public static final String PROP_REWRITE_MAVEN_PLUGIN_VERSION = "rewrite-maven-plugin-version";
    public static final String PROP_REWRITE_GRADLE_PLUGIN_VERSION = "rewrite-gradle-plugin-version";

    public static FetchResult fetchRecipes(MessageWriter log, MavenArtifactResolver artifactResolver, BuildTool buildTool,
            String recipeVersion, String currentVersion,
            String targetVersion) {
        final String gav = QUARKUS_RECIPE_GA + ":" + recipeVersion;
        try {
            final Artifact artifact = artifactResolver.resolve(DependencyUtils.toArtifact(gav)).getArtifact();
            final ResourceLoader resourceLoader = ResourceLoaders.resolveFileResourceLoader(
                    artifact.getFile());
            final List<String> recipes = resourceLoader.loadResourceAsPath("quarkus-updates/core",
                    path -> {
                        try (final Stream<Path> pathStream = Files.walk(path)) {
                            return pathStream
                                    .filter(p -> p.getFileName().toString().matches("^\\d\\H+.ya?ml$"))
                                    .filter(p -> shouldApplyRecipe(p.getFileName().toString(), currentVersion, targetVersion))
                                    .map(p -> {
                                        try {
                                            return new String(Files.readAllBytes(p));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }).collect(Collectors.toList());
                        }
                    });
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
            final String propRewritePluginVersion = getPropRewritePluginVersion(props, buildTool);

            log.info(String.format(
                    "Resolved io.quarkus:quarkus-updates-recipes:%s with %s recipe(s) to update from %s to %s (initially made for OpenRewrite %s plugin version: %s) ",
                    artifact.getVersion(),
                    recipes.size(),
                    currentVersion,
                    targetVersion,
                    buildTool,
                    propRewritePluginVersion));
            return new FetchResult(artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion(),
                    recipes, propRewritePluginVersion);
        } catch (BootstrapMavenException e) {
            throw new RuntimeException("Failed to resolve artifact: " + gav, e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load recipes in artifact: " + gav, e);
        }
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
        String recipeVersion = recipeFileName.replaceFirst("[.][^.]+$", "");
        final DefaultArtifactVersion recipeAVersion = new DefaultArtifactVersion(recipeVersion);
        final DefaultArtifactVersion currentAVersion = new DefaultArtifactVersion(currentVersion);
        final DefaultArtifactVersion targetAVersion = new DefaultArtifactVersion(targetVersion);
        return currentAVersion.compareTo(recipeAVersion) < 0 && targetAVersion.compareTo(recipeAVersion) >= 0;
    }
}
