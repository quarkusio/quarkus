package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.messagewriter.MessageIcons.UP_TO_DATE_ICON;
import static io.quarkus.devtools.project.state.ProjectStates.resolveProjectState;
import static io.quarkus.devtools.project.update.ProjectUpdateInfos.resolvePlatformUpdateInfo;
import static io.quarkus.devtools.project.update.ProjectUpdateInfos.resolveRecommendedState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.devtools.commands.UpdateProject;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.JavaVersion;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.project.state.ProjectState;
import io.quarkus.devtools.project.update.*;
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
    private static final Logger log = LoggerFactory.getLogger(UpdateProjectCommandHandler.class);

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
        final String targetPlatformVersion = invocation.getValue(UpdateProject.TARGET_PLATFORM_VERSION);
        final boolean perModule = invocation.getValue(UpdateProject.PER_MODULE, false);
        final ProjectState currentState = resolveProjectState(appModel,
                invocation.getQuarkusProject().getExtensionsCatalog());
        final ArtifactCoords projectQuarkusPlatformBom = getProjectQuarkusPlatformBOM(currentState);
        if (projectQuarkusPlatformBom == null) {
            String error = "The project does not import any Quarkus platform BOM";

            invocation.log().error(error);
            return QuarkusCommandOutcome.failure(error);
        }
        final QuarkusProject quarkusProject = invocation.getQuarkusProject();
        final ProjectState recommendedState = resolveRecommendedState(currentState, targetCatalog,
                invocation.log());
        final ProjectPlatformUpdateInfo platformUpdateInfo = resolvePlatformUpdateInfo(currentState,
                recommendedState);
        final ProjectExtensionsUpdateInfo extensionsUpdateInfo = ProjectUpdateInfos.resolveExtensionsUpdateInfo(
                currentState,
                recommendedState);
        boolean shouldUpdate = logUpdates(invocation.getQuarkusProject(), currentState, recommendedState, platformUpdateInfo,
                extensionsUpdateInfo,
                false, perModule,
                quarkusProject.log());
        final boolean noRewrite = invocation.getValue(UpdateProject.NO_REWRITE, false);
        if (shouldUpdate && !noRewrite) {
            final BuildTool buildTool = quarkusProject.getExtensionManager().getBuildTool();
            String kotlinVersion = getMetadata(targetCatalog, "project", "properties", "kotlin-version");
            final Optional<Integer> updateJavaVersion = resolveUpdateJavaVersion(extensionsUpdateInfo, projectJavaVersion);
            QuarkusUpdates.ProjectUpdateRequest request = new QuarkusUpdates.ProjectUpdateRequest(
                    buildTool,
                    projectQuarkusPlatformBom.getVersion(),
                    targetPlatformVersion,
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
                log.info("Running OpenRewrite recipe...");
                log.info("");

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

        return QuarkusCommandOutcome.success();
    }

    private static Optional<Integer> resolveUpdateJavaVersion(ProjectExtensionsUpdateInfo extensionsUpdateInfo,
            JavaVersion projectJavaVersion) {
        final OptionalInt minJavaVersion = extensionsUpdateInfo.getMinJavaVersion();
        final Optional<Integer> updateJavaVersion;
        if (minJavaVersion.isPresent()
                && projectJavaVersion.isPresent()
                && minJavaVersion.getAsInt() > projectJavaVersion.getAsInt()) {
            updateJavaVersion = Optional.of(minJavaVersion.getAsInt());
        } else {
            updateJavaVersion = Optional.empty();
        }
        return updateJavaVersion;
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

    private static boolean logUpdates(QuarkusProject project, ProjectState currentState, ProjectState recommendedState,
            ProjectPlatformUpdateInfo platformUpdateInfo,
            ProjectExtensionsUpdateInfo extensionsUpdateInfo, boolean recommendState,
            boolean perModule, MessageWriter log) {
        printSeparator(log);
        if (currentState.getPlatformBoms().isEmpty()) {
            log.info("The project does not import any Quarkus platform BOM");
            printSeparator(log);
            return false;
        }
        if (currentState.getExtensions().isEmpty()) {
            log.info("No Quarkus extensions were found among the project dependencies");
            printSeparator(log);
            return false;
        }
        if (currentState == recommendedState) {
            log.info("The project is up-to-date");
            printSeparator(log);
            return false;
        }

        if (recommendState) {
            ProjectInfoCommandHandler.logState(recommendedState, perModule, false, log);
            printSeparator(log);
            return false;
        }

        if (platformUpdateInfo.isPlatformUpdatesAvailable()) {
            log.info("Quarkus platform BOM(s) to update:");
            if (!platformUpdateInfo.getImportVersionUpdates().isEmpty()) {
                for (PlatformInfo importInfo : platformUpdateInfo.getImportVersionUpdates()) {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                            "", importInfo.getImported().getKey().toGacString() + ":["
                                    + importInfo.getImported().getVersion() + " -> " + importInfo.getRecommendedVersion()
                                    + "]"));
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
        } else if (!extensionsUpdateInfo.shouldUpdateExtensions()) {
            log.info("The project is up-to-date " + MessageIcons.UP_TO_DATE_ICON.iconOrMessage());
            printSeparator(log);
            return false;
        } else {
            log.info("Quarkus platform BOM(s) are up-to-date " + MessageIcons.UP_TO_DATE_ICON.iconOrMessage());
            log.info("");
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
            final List<ExtensionUpdateInfo> extensions = extensionsUpdateInfo.extensionsByProvider().getOrDefault(provider,
                    Collections.emptyList()).stream().filter(ExtensionUpdateInfo::isUpdateRecommended).toList();
            if (extensions.isEmpty()) {
                continue;
            }
            log.info("Extensions from " + platform.getRecommendedProviderKey() + " to update:");
            for (ExtensionUpdateInfo e : extensions) {

                final ExtensionUpdateInfo.VersionUpdateType versionUpdateType = e.getVersionUpdateType();

                if (e.hasKeyChanged()) {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                            "",
                            updateInfo(e)));
                } else {
                    switch (versionUpdateType) {
                        case PLATFORM_MANAGED:
                            // The extension update is done when updating the platform
                            log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "",
                                    e.getCurrentDep().getArtifact().getKey().toGacString())
                                    + " (already managed by platform BOM)");
                            break;
                        case RECOMMEND_PLATFORM_MANAGED:
                            log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                    "",
                                    e.getCurrentDep().getArtifact().toCompactCoords()
                                            + " -> drop version (managed by platform)"));
                            break;
                        case ADD_VERSION:
                            log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                    "",
                                    e.getRecommendedDependency().getArtifact().toCompactCoords()
                                            + " -> add version (managed by platform)"));
                            break;
                        case UPDATE_VERSION:
                            log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                    "",
                                    updateInfo(e)));
                            break;
                    }
                }
            }
            log.info("");
        }
        final List<ExtensionUpdateInfo> simpleVersionUpdates = extensionsUpdateInfo.getSimpleVersionUpdates();
        if (!simpleVersionUpdates.isEmpty()) {
            log.info("Other extensions to update:");
            for (ExtensionUpdateInfo u : simpleVersionUpdates) {
                if (u.getVersionUpdateType() == ExtensionUpdateInfo.VersionUpdateType.PLATFORM_MANAGED) {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                            "",
                            u.getCurrentDep().getArtifact().toCompactCoords()
                                    + " -> drop version (managed by platform)"));
                } else {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                            "",
                            updateInfo(u)));
                }
            }
        }

        printSeparator(log);
        return true;
    }

    private static void printSeparator(MessageWriter log) {
        log.info("");
        log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        log.info("");
    }

    private static String updateInfo(ExtensionUpdateInfo e) {
        return e.getCurrentDep().getArtifact().getKey().toGacString() + ":[" + e.getCurrentDep().getVersion() + " -> "
                + e.getRecommendedDependency().getVersion() + "]";
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
