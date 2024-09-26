package io.quarkus.devtools.project.update.rewrite;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.ExtensionUpdateInfo;
import io.quarkus.devtools.project.update.PlatformInfo;
import io.quarkus.devtools.project.update.ProjectExtensionsUpdateInfo;
import io.quarkus.devtools.project.update.ProjectPlatformUpdateInfo;
import io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.FetchResult;
import io.quarkus.devtools.project.update.rewrite.operations.AddManagedDependencyOperation;
import io.quarkus.devtools.project.update.rewrite.operations.ChangeDependencyOperation;
import io.quarkus.devtools.project.update.rewrite.operations.DropDependencyVersionOperation;
import io.quarkus.devtools.project.update.rewrite.operations.RemoveManagedDependencyOperation;
import io.quarkus.devtools.project.update.rewrite.operations.UpdateDependencyVersionOperation;
import io.quarkus.devtools.project.update.rewrite.operations.UpdateJavaVersionOperation;
import io.quarkus.devtools.project.update.rewrite.operations.UpdatePropertyOperation;
import io.quarkus.devtools.project.update.rewrite.operations.UpgradeGradlePluginOperation;

public final class QuarkusUpdates {

    private static final String IMPORT_SCOPE = "import";

    private QuarkusUpdates() {
    }

    public static FetchResult createRecipe(MessageWriter log, Path target, MavenArtifactResolver artifactResolver,
            BuildTool buildTool, String quarkusUpdateRecipes, String additionalUpdateRecipes,
            ProjectUpdateRequest request)
            throws IOException {
        final FetchResult result = QuarkusUpdatesRepository.fetchRecipes(
                log,
                artifactResolver,
                buildTool,
                quarkusUpdateRecipes,
                additionalUpdateRecipes,
                request.currentVersion,
                request.targetVersion,
                request.projectExtensionsUpdateInfo
                        .getVersionUpdates());
        QuarkusUpdateRecipe recipe = new QuarkusUpdateRecipe()
                .buildTool(request.buildTool);
        if (request.updateJavaVersion.isPresent()) {
            final String javaVersion = request.updateJavaVersion.get().toString();
            recipe.addOperation(new UpdateJavaVersionOperation(javaVersion));
        }
        switch (request.buildTool) {
            case MAVEN:
                // Properly handle the Platform updates
                for (PlatformInfo platformInfo : request.platformUpdateInfo.getPlatformImports().values()) {
                    if (platformInfo.getRecommended() == null) {
                        recipe.addOperation(new RemoveManagedDependencyOperation(platformInfo.getImported().getGroupId(),
                                platformInfo.getImported().getArtifactId()));
                    } else if (platformInfo.getImported() == null) {
                        // we need to add the Platform
                        recipe.addOperation(new AddManagedDependencyOperation(platformInfo.getRecommended().getGroupId(),
                                platformInfo.getRecommended().getArtifactId(), platformInfo.getRecommended().getVersion(),
                                platformInfo.getRecommended().getType(), IMPORT_SCOPE));
                        continue;
                    } else {
                        // usual case when we should update the Platform
                        if (platformInfo.getRecommended().getKey().equals(platformInfo.getImported().getKey())) {
                            // same Platform, we can just update the version
                            recipe.addOperation(new UpdateDependencyVersionOperation("io.quarkus.platform", "quarkus-bom",
                                    request.targetVersion));
                        } else {
                            // we need to update the Platform coordinates
                            recipe.addOperation(new ChangeDependencyOperation(platformInfo.getImported().getGroupId(),
                                    platformInfo.getImported().getArtifactId(),
                                    platformInfo.getRecommended().getGroupId(), platformInfo.getRecommended().getArtifactId(),
                                    platformInfo.getRecommended().getVersion()));
                        }
                    }
                }

                // last resort for projects not following conventions or using the old Universe BOM
                recipe.addOperation(new UpdateDependencyVersionOperation("io.quarkus", "quarkus-universe-bom",
                                request.targetVersion))
                        .addOperation(new UpdatePropertyOperation("quarkus.platform.version", request.targetVersion))
                        .addOperation(new UpdatePropertyOperation("quarkus.version", request.targetVersion))
                        .addOperation(new UpdatePropertyOperation("quarkus-plugin.version", request.targetVersion));
                if (request.kotlinVersion != null) {
                    recipe.addOperation(new UpdatePropertyOperation("kotlin.version", request.kotlinVersion));
                }
                break;
            case GRADLE:
            case GRADLE_KOTLIN_DSL:
                // Unfortunately, for now we can't really handle Platforms properly in Gradle due to OpenRewrite limitations
                // Using a naive approach to replace the properties

                recipe.addOperation(new UpdatePropertyOperation("quarkusPlatformVersion", request.targetVersion))
                        .addOperation(new UpdatePropertyOperation("quarkusPluginVersion", request.targetVersion));
                if (request.kotlinVersion != null) {
                    recipe.addOperation(
                            new UpgradeGradlePluginOperation("org.jetbrains.kotlin.*", request.kotlinVersion));
                }
                break;
        }

        for (ExtensionUpdateInfo versionUpdates : request.projectExtensionsUpdateInfo
                .getSimpleVersionUpdates()) {
            if (versionUpdates.getVersionUpdateType()
                    .equals(ExtensionUpdateInfo.VersionUpdateType.RECOMMEND_PLATFORM_MANAGED)) {
                recipe.addOperation(new DropDependencyVersionOperation(
                        versionUpdates.getCurrentDep().getArtifact().getGroupId(),
                        versionUpdates.getCurrentDep().getArtifact().getArtifactId()));
            } else {
                recipe.addOperation(new UpdateDependencyVersionOperation(
                        versionUpdates.getCurrentDep().getArtifact().getGroupId(),
                        versionUpdates.getCurrentDep().getArtifact().getArtifactId(),
                        versionUpdates.getRecommendedDependency().getVersion()));
            }

        }

        for (String s : result.getRecipes()) {
            recipe.addRecipes(QuarkusUpdateRecipeIO.readRecipesYaml(s));
        }

        QuarkusUpdateRecipeIO.write(target, recipe);
        return result;
    }

    public static class ProjectUpdateRequest {

        public final BuildTool buildTool;
        public final String currentVersion;
        public final String targetVersion;
        public final ProjectPlatformUpdateInfo platformUpdateInfo;
        public final String kotlinVersion;
        public final Optional<Integer> updateJavaVersion;
        public final ProjectExtensionsUpdateInfo projectExtensionsUpdateInfo;

        public ProjectUpdateRequest(String currentVersion, String targetVersion,
                ProjectPlatformUpdateInfo platformUpdateInfo, String kotlinVersion,
                Optional<Integer> updateJavaVersion, ProjectExtensionsUpdateInfo projectExtensionsUpdateInfo) {
            this(BuildTool.MAVEN, currentVersion, targetVersion, platformUpdateInfo, kotlinVersion, updateJavaVersion,
                    projectExtensionsUpdateInfo);
        }

        public ProjectUpdateRequest(BuildTool buildTool, String currentVersion, String targetVersion,
                ProjectPlatformUpdateInfo platformUpdateInfo,
                String kotlinVersion,
                Optional<Integer> updateJavaVersion, ProjectExtensionsUpdateInfo projectExtensionsUpdateInfo) {
            this.buildTool = buildTool;
            this.currentVersion = currentVersion;
            this.targetVersion = targetVersion;
            this.platformUpdateInfo = platformUpdateInfo;
            this.kotlinVersion = kotlinVersion;
            this.updateJavaVersion = updateJavaVersion;
            this.projectExtensionsUpdateInfo = projectExtensionsUpdateInfo;
        }
    }
}
