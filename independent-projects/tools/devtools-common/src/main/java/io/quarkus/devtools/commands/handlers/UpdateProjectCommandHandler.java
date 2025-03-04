package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.project.state.ProjectStates.resolveProjectState;
import static io.quarkus.devtools.project.update.ProjectUpdateInfos.resolvePlatformUpdateInfo;
import static io.quarkus.devtools.project.update.ProjectUpdateInfos.resolveRecommendedState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.devtools.commands.UpdateProject;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.JavaVersion;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.project.state.ProjectState;
import io.quarkus.devtools.project.update.ExtensionUpdateInfo;
import io.quarkus.devtools.project.update.PlatformInfo;
import io.quarkus.devtools.project.update.ProjectExtensionsUpdateInfo;
import io.quarkus.devtools.project.update.ProjectPlatformUpdateInfo;
import io.quarkus.devtools.project.update.ProjectUpdateInfos;
import io.quarkus.devtools.project.update.rewrite.QuarkusUpdateCommand;
import io.quarkus.devtools.project.update.rewrite.QuarkusUpdates;
import io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository;
import io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.FetchResult;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.registry.catalog.ExtensionCatalog;

public class UpdateProjectCommandHandler implements QuarkusCommandHandler {
    public static final String ADD = "Add:";
    public static final String REMOVE = "Remove:";
    public static final String UPDATE = "Update:";

    public static final String ITEM_FORMAT = "%-7s %s";

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        final JavaVersion projectJavaVersion = invocation.getQuarkusProject().getJavaVersion();
        invocation.log().info("Detected project Java version: %s", projectJavaVersion);
        if (projectJavaVersion.isEmpty()) {
            String instruction = invocation.getQuarkusProject().getBuildTool().isAnyGradle() ? "java>targetCompatibility"
                    : "maven.compiler.release property";
            String error = String.format("Project Java version not detected, set %s in your build file to fix the error.",
                    instruction);

            invocation.log().error(error);
            return QuarkusCommandOutcome.failure(error);
        } else {
            invocation.log().info("Detected project Java version: %s", projectJavaVersion);
        }
        final ApplicationModel appModel = invocation.getValue(UpdateProject.APP_MODEL);
        final ExtensionCatalog targetCatalog = invocation.getValue(UpdateProject.TARGET_CATALOG);
        final boolean perModule = invocation.getValue(UpdateProject.PER_MODULE, false);

        final ProjectState currentState = resolveProjectState(appModel,
                invocation.getQuarkusProject().getExtensionsCatalog());
        final ArtifactCoords currentQuarkusPlatformBom = getProjectQuarkusPlatformBOM(currentState);
        var failure = ensureQuarkusBomVersionIsNotNull(currentQuarkusPlatformBom, invocation.log());
        if (failure != null) {
            return failure;
        }

        final ProjectState recommendedState = resolveRecommendedState(currentState, targetCatalog,
                invocation.log());
        final ArtifactCoords recommendedQuarkusPlatformBom = getProjectQuarkusPlatformBOM(recommendedState);
        failure = ensureQuarkusBomVersionIsNotNull(recommendedQuarkusPlatformBom, invocation.log());
        if (failure != null) {
            return failure;
        }

        if (Objects.equals(currentQuarkusPlatformBom, recommendedQuarkusPlatformBom)) {
            ProjectInfoCommandHandler.logState(currentState, perModule, true, invocation.getQuarkusProject().log());
        } else {
            invocation.log().info("Instructions to update this project from '%s' to '%s':",
                    currentQuarkusPlatformBom, recommendedQuarkusPlatformBom);
            final QuarkusProject quarkusProject = invocation.getQuarkusProject();
            final ProjectPlatformUpdateInfo platformUpdateInfo = resolvePlatformUpdateInfo(currentState,
                    recommendedState);
            final ProjectExtensionsUpdateInfo extensionsUpdateInfo = ProjectUpdateInfos.resolveExtensionsUpdateInfo(
                    currentState,
                    recommendedState);

            logUpdates(invocation.getQuarkusProject(), currentState, recommendedState, platformUpdateInfo,
                    extensionsUpdateInfo,
                    false, perModule,
                    invocation.log());
            final boolean noRewrite = invocation.getValue(UpdateProject.NO_REWRITE, false);

            if (!noRewrite) {
                final BuildTool buildTool = quarkusProject.getExtensionManager().getBuildTool();
                String kotlinVersion = getMetadata(targetCatalog, "project", "properties", "kotlin-version");
                final OptionalInt minJavaVersion = extensionsUpdateInfo.getMinJavaVersion();
                final Optional<Integer> updateJavaVersion;
                if (minJavaVersion.isPresent()
                        && projectJavaVersion.isPresent()
                        && minJavaVersion.getAsInt() > projectJavaVersion.getAsInt()) {
                    updateJavaVersion = Optional.of(minJavaVersion.getAsInt());
                } else {
                    updateJavaVersion = Optional.empty();
                }
                QuarkusUpdates.ProjectUpdateRequest request = new QuarkusUpdates.ProjectUpdateRequest(
                        buildTool,
                        currentQuarkusPlatformBom.getVersion(),
                        recommendedQuarkusPlatformBom.getVersion(),
                        kotlinVersion,
                        updateJavaVersion,
                        extensionsUpdateInfo);
                Path recipe = null;
                try {
                    recipe = Files.createTempFile("quarkus-project-recipe-", ".yaml");
                    final String quarkusUpdateRecipes = invocation.getValue(
                            UpdateProject.REWRITE_QUARKUS_UPDATE_RECIPES,
                            QuarkusUpdatesRepository.DEFAULT_UPDATE_RECIPES_VERSION);
                    final String additionalUpdateRecipes = invocation.getValue(
                            UpdateProject.REWRITE_ADDITIONAL_UPDATE_RECIPES,
                            null);
                    final FetchResult fetchResult = QuarkusUpdates.createRecipe(invocation.log(), recipe,
                            QuarkusProjectHelper.artifactResolver(), buildTool, quarkusUpdateRecipes,
                            additionalUpdateRecipes, request);
                    invocation.log().info("OpenRewrite recipe generated: %s", recipe);

                    String rewritePluginVersion = invocation.getValue(UpdateProject.REWRITE_PLUGIN_VERSION,
                            fetchResult.getRewritePluginVersion());
                    boolean rewriteDryRun = invocation.getValue(UpdateProject.REWRITE_DRY_RUN, false);
                    QuarkusUpdateCommand.handle(
                            invocation.log(),
                            buildTool,
                            quarkusProject.getProjectDirPath(),
                            rewritePluginVersion,
                            fetchResult.getRecipesGAV(),
                            recipe,
                            rewriteDryRun);

                } catch (IOException e) {
                    throw new QuarkusCommandException("Error while generating the project update script", e);
                }
            }
        }
        return QuarkusCommandOutcome.success();
    }

    private static ArtifactCoords getProjectQuarkusPlatformBOM(ProjectState currentState) {
        for (ArtifactCoords c : currentState.getPlatformBoms()) {
            if (c.getArtifactId().equals(ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID)
                    || c.getArtifactId().equals(ToolsConstants.UNIVERSE_PLATFORM_BOM_ARTIFACT_ID)) {
                return c;
            }
        }
        return null;
    }

    private static QuarkusCommandOutcome ensureQuarkusBomVersionIsNotNull(ArtifactCoords bomCoords, MessageWriter log) {
        if (bomCoords == null) {
            String error = "The project state is missing the Quarkus platform BOM";
            log.error(error);
            return QuarkusCommandOutcome.failure(error);
        }
        return null;
    }

    private static void logUpdates(QuarkusProject project, ProjectState currentState, ProjectState recommendedState,
            ProjectPlatformUpdateInfo platformUpdateInfo,
            ProjectExtensionsUpdateInfo extensionsUpdateInfo, boolean recommendState,
            boolean perModule, MessageWriter log) {
        if (currentState.getPlatformBoms().isEmpty()) {
            log.info("The project does not import any Quarkus platform BOM");
            return;
        }
        if (currentState.getExtensions().isEmpty()) {
            log.info("No Quarkus extensions were found among the project dependencies");
            return;
        }
        if (currentState == recommendedState) {
            log.info("The project is up-to-date");
            return;
        }

        if (recommendState) {
            ProjectInfoCommandHandler.logState(recommendedState, perModule, false, log);
            return;
        }
        if (platformUpdateInfo.isPlatformUpdatesAvailable()) {
            log.info("Recommended Quarkus platform BOM updates:");
            if (!platformUpdateInfo.getImportVersionUpdates().isEmpty()) {
                for (PlatformInfo importInfo : platformUpdateInfo.getImportVersionUpdates()) {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                            UpdateProjectCommandHandler.UPDATE, importInfo.getImported().toCompactCoords()) + " -> "
                            + importInfo.getRecommendedVersion());
                }
            }
            if (!platformUpdateInfo.getNewImports().isEmpty()) {
                for (PlatformInfo importInfo : platformUpdateInfo.getNewImports()) {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                            UpdateProjectCommandHandler.ADD, importInfo.getRecommended().toCompactCoords()));
                }
            }
            if (platformUpdateInfo.isImportsToBeRemoved()) {
                for (PlatformInfo importInfo : platformUpdateInfo.getPlatformImports().values()) {
                    if (importInfo.getRecommended() == null) {
                        log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                UpdateProjectCommandHandler.REMOVE, importInfo.getImported().toCompactCoords()));
                    }
                }
            }
            log.info("");
        }

        if (extensionsUpdateInfo.shouldUpdateExtensions() && !platformUpdateInfo.isPlatformUpdatesAvailable()) {
            log.info("The project is up-to-date");
            return;
        }

        if (extensionsUpdateInfo.getMinJavaVersion().isPresent() && project.getJavaVersion().isPresent()) {
            final Integer extensionsMinJavaVersion = extensionsUpdateInfo.getMinJavaVersion().getAsInt();
            if (extensionsMinJavaVersion > project.getJavaVersion().getAsInt()) {
                log.warn("We detected that some of the updated extensions require an update of the Java version to: %s",
                        extensionsMinJavaVersion);
            }
        }

        for (PlatformInfo platform : platformUpdateInfo.getPlatformImports().values()) {
            final String provider = platform.getRecommendedProviderKey();
            if (!extensionsUpdateInfo.containsProvider(provider)) {
                continue;
            }
            log.info("Extensions from " + platform.getRecommendedProviderKey() + ":");

            for (ExtensionUpdateInfo e : extensionsUpdateInfo.extensionsByProvider().getOrDefault(provider,
                    Collections.emptyList())) {

                final ExtensionUpdateInfo.VersionUpdateType versionUpdateType = e.getVersionUpdateType();

                if (e.hasKeyChanged()) {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                            UpdateProjectCommandHandler.UPDATE,
                            e.getCurrentDep().getArtifact().toCompactCoords() + " -> "
                                    + e.getRecommendedDependency().getArtifact().toCompactCoords()));
                } else {
                    switch (versionUpdateType) {
                        case PLATFORM_MANAGED:
                            // The extension update is done when updating the platform
                            break;
                        case RECOMMEND_PLATFORM_MANAGED:
                            log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                    UpdateProjectCommandHandler.UPDATE,
                                    e.getCurrentDep().getArtifact().toCompactCoords()
                                            + " -> drop version (managed by platform)"));
                            break;
                        case ADD_VERSION:
                            log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                    UpdateProjectCommandHandler.UPDATE,
                                    e.getRecommendedDependency().getArtifact().toCompactCoords()
                                            + " -> add version (managed by platform)"));
                            break;
                        case UPDATE_VERSION:
                            log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                    UpdateProjectCommandHandler.UPDATE,
                                    e.getCurrentDep().getArtifact().toCompactCoords() + " -> "
                                            + e.getRecommendedDependency().getVersion()));
                            break;
                    }
                }
            }
            log.info("");
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> T getMetadata(ExtensionCatalog catalog, String... path) {
        Object currentValue = catalog.getMetadata();
        for (String pathElement : path) {
            if (!(currentValue instanceof Map)) {
                return null;
            }

            currentValue = ((Map) currentValue).get(pathElement);
        }

        return (T) currentValue;
    }

}
