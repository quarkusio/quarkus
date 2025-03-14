package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.messagewriter.MessageIcons.UP_TO_DATE_ICON;
import static io.quarkus.devtools.project.state.ProjectStates.resolveProjectState;
import static io.quarkus.devtools.project.update.ProjectUpdateInfos.resolvePlatformUpdateInfo;
import static io.quarkus.devtools.project.update.ProjectUpdateInfos.resolveRecommendedState;

import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.devtools.commands.UpdateProject;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageFormatter;
import io.quarkus.devtools.messagewriter.MessageIcons;
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

    public static final String ITEM_FORMAT = " %-7s %s";

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        final JavaVersion projectJavaVersion = invocation.getQuarkusProject().getJavaVersion();
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

        final ProjectPlatformUpdateInfo platformUpdateInfo = resolvePlatformUpdateInfo(currentState,
                recommendedState);
        final ProjectExtensionsUpdateInfo extensionsUpdateInfo = ProjectUpdateInfos.resolveExtensionsUpdateInfo(
                currentState,
                recommendedState);
        boolean shouldUpdate = logUpdates(invocation.getQuarkusProject(), currentState, recommendedState, platformUpdateInfo,
                extensionsUpdateInfo,
                invocation.log());
        Boolean rewrite = invocation.getValue(UpdateProject.REWRITE, null);
        boolean rewriteDryRun = invocation.getValue(UpdateProject.REWRITE_DRY_RUN, false);
        if (shouldUpdate) {
            final QuarkusProject quarkusProject = invocation.getQuarkusProject();
            final BuildTool buildTool = quarkusProject.getExtensionManager().getBuildTool();
            // TODO targetCatalog shouldn't be used here, since it might not be the recommended one according to the calculated recommended state
            String kotlinVersion = getMetadata(targetCatalog, "project", "properties", "kotlin-version");
            final Optional<Integer> updateJavaVersion = resolveUpdateJavaVersion(extensionsUpdateInfo, projectJavaVersion);
            QuarkusUpdates.ProjectUpdateRequest request = new QuarkusUpdates.ProjectUpdateRequest(
                    buildTool,
                    currentQuarkusPlatformBom.getVersion(),
                    recommendedQuarkusPlatformBom.getVersion(),
                    kotlinVersion,
                    updateJavaVersion,
                    extensionsUpdateInfo);
            Path recipe = null;
            try {
                final Path projectDir = invocation.getQuarkusProject().getProjectDirPath();
                final Path buildDir = projectDir
                        .resolve(invocation.getQuarkusProject().getBuildTool().getBuildDirectory());
                final Path rewriteDir = buildDir.resolve("rewrite");
                recipe = rewriteDir.resolve("rewrite.yaml");
                Files.deleteIfExists(recipe);
                Files.createDirectories(recipe.getParent());
                final String quarkusUpdateRecipes = invocation.getValue(
                        UpdateProject.REWRITE_QUARKUS_UPDATE_RECIPES,
                        QuarkusUpdatesRepository.DEFAULT_UPDATE_RECIPES_VERSION);
                final String additionalUpdateRecipes = invocation.getValue(
                        UpdateProject.REWRITE_ADDITIONAL_UPDATE_RECIPES,
                        null);
                final FetchResult fetchResult = QuarkusUpdates.createRecipe(invocation.log(), recipe,
                        QuarkusProjectHelper.artifactResolver(), buildTool, quarkusUpdateRecipes,
                        additionalUpdateRecipes, request);
                quarkusProject.log().info("");
                quarkusProject.log()
                        .info("We have generated a recipe file to update your project (version updates + specific recipes):");
                quarkusProject.log().info(MessageFormatter.underline(projectDir.relativize(recipe).toString()));
                quarkusProject.log().info("");

                if (rewriteDryRun) {
                    rewrite = true;
                }

                if (rewrite == null) {
                    CompletableFuture<String> userInputFuture = CompletableFuture
                            .supplyAsync(() -> askUserConfirmationForUpdate(invocation.log()));
                    try {
                        final String userInput = userInputFuture.get(2, TimeUnit.MINUTES).toLowerCase().trim();
                        if (userInput.equalsIgnoreCase("y")) {
                            rewrite = true;
                        } else if (userInput.equalsIgnoreCase("d")) {
                            rewriteDryRun = true;
                            rewrite = true;
                        } else {
                            quarkusProject.log().info("");
                            quarkusProject.log().info("Project update has been skipped.");
                            rewrite = false;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } catch (ExecutionException | TimeoutException e) {
                        quarkusProject.log().info("");
                        quarkusProject.log().info("Project update has been skipped after timeout.");
                        rewrite = false;
                    }
                }

                if (rewrite) {
                    String rewritePluginVersion = invocation.getValue(UpdateProject.REWRITE_PLUGIN_VERSION,
                            fetchResult.getRewritePluginVersion());
                    quarkusProject.log().info("");
                    Path logFile = recipe.getParent().resolve("rewrite.log");

                    QuarkusUpdateCommand.handle(
                            invocation.log(),
                            buildTool,
                            quarkusProject.getProjectDirPath(),
                            rewritePluginVersion,
                            fetchResult.getRecipesGAV(),
                            recipe,
                            logFile,
                            rewriteDryRun);
                    final Path patchFile = rewriteDir.resolve("rewrite.patch");
                    if (rewriteDryRun && Files.isRegularFile(patchFile)) {
                        quarkusProject.log().info("Patch file available:");
                        quarkusProject.log().info(MessageFormatter.underline(projectDir.relativize(patchFile).toString()));
                        quarkusProject.log().info("");
                    }

                } else {
                    printSeparator(quarkusProject.log());
                }

            } catch (IOException e) {
                throw new QuarkusCommandException("Error while generating the project update script", e);
            }
        }

        return QuarkusCommandOutcome.success();
    }

    private static String askUserConfirmationForUpdate(MessageWriter log) {
        System.out.print(System.lineSeparator() +
                MessageFormatter.bold(
                        "Do you want to apply the generated update recipes with OpenRewrite?")
                + " ([" + MessageFormatter.green("y") + "]es, [" + MessageFormatter.red("n") + "]o, ["
                + MessageFormatter.blue("d") + "]ry-run + [Enter])"
                + System.lineSeparator() + System.lineSeparator());
        try (Scanner scanner = new Scanner(new FilterInputStream(System.in) {
            @Override
            public void close() throws IOException {
                //don't close System.in!
            }
        })) {
            return scanner.nextLine();
        } catch (Exception e) {
            log.debug("Unable to get user confirmation", e);
            return "";
        }
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

    private static QuarkusCommandOutcome<Void> ensureQuarkusBomVersionIsNotNull(ArtifactCoords bomCoords, MessageWriter log) {
        if (bomCoords == null) {
            String error = "The project state is missing the Quarkus platform BOM";
            log.error(error);
            return QuarkusCommandOutcome.failure(error);
        }
        return null;
    }

    private static boolean logUpdates(QuarkusProject project, ProjectState currentState, ProjectState recommendedState,
            ProjectPlatformUpdateInfo platformUpdateInfo,
            ProjectExtensionsUpdateInfo extensionsUpdateInfo,
            MessageWriter log) {
        printSeparator(log);
        if (currentState.getPlatformBoms().isEmpty()) {
            log.info(MessageFormatter.red("The project does not import any Quarkus platform BOM"));
            printSeparator(log);
            return false;
        }
        if (currentState.getExtensions().isEmpty()) {
            log.info("No Quarkus extensions were found among the project dependencies");
            printSeparator(log);
            return false;
        }
        if (currentState == recommendedState) {
            log.info(MessageFormatter.green("The project is up-to-date)"));
            printSeparator(log);
            return false;
        }

        if (platformUpdateInfo.isPlatformUpdatesAvailable()) {
            log.info(MessageFormatter.bold("Suggested Quarkus platform BOM updates:"));
            if (!platformUpdateInfo.getImportVersionUpdates().isEmpty()) {
                for (PlatformInfo importInfo : platformUpdateInfo.getImportVersionUpdates()) {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "~",
                            importInfo.getImported().getKey().toGacString() + ":pom:["
                                    + MessageFormatter.red(importInfo.getImported().getVersion()) + " -> "
                                    + MessageFormatter.green(importInfo.getRecommendedVersion())
                                    + "]"));
                }
            }
            if (platformUpdateInfo.isImportsToBeRemoved()) {
                for (PlatformInfo importInfo : platformUpdateInfo.getPlatformImports().values()) {
                    if (importInfo.getRecommended() == null) {
                        log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "-",
                                "[" + MessageFormatter.red(importInfo.getImported().toCompactCoords()) + "]"));
                    }
                }
            }
            if (!platformUpdateInfo.getNewImports().isEmpty()) {
                for (PlatformInfo importInfo : platformUpdateInfo.getNewImports()) {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "+",
                            "[" + MessageFormatter.green(importInfo.getRecommended().toCompactCoords()) + "]"));
                }
            }
            log.info("");
        } else if (!extensionsUpdateInfo.shouldUpdateExtensions()) {
            log.info(MessageFormatter.green("The project is up-to-date " + MessageIcons.UP_TO_DATE_ICON.iconOrMessage()));
            printSeparator(log);
            return false;
        } else {
            log.info(MessageFormatter
                    .green("Quarkus platform BOMs are up-to-date " + MessageIcons.UP_TO_DATE_ICON.iconOrMessage()));
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
            log.info(MessageFormatter.bold(
                    "Suggested extensions updates for '%s':".formatted(platform.getRecommendedProviderKey())));
            for (ExtensionUpdateInfo e : extensions) {

                final ExtensionUpdateInfo.VersionUpdateType versionUpdateType = e.getVersionUpdateType();

                if (e.hasKeyChanged()) {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "~", updateInfo(e)));
                } else {
                    switch (versionUpdateType) {
                        case PLATFORM_MANAGED:
                            // The extension update is done when updating the platform
                            log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, UP_TO_DATE_ICON.iconOrMessage(),
                                    e.getCurrentDep().getArtifact().getKey().toGacString()
                                            + " (synced with BOM)"));
                            break;
                        case RECOMMEND_PLATFORM_MANAGED:
                            log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "-",
                                    e.getCurrentDep().getArtifact().getKey().toGacString()) + ":["
                                    + MessageFormatter.red(e.getCurrentDep().getVersion()) + " -> "
                                    + MessageFormatter.green("managed") + "]");
                            break;
                        case ADD_VERSION:
                            log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, versionUpdateType.equals(
                                    ExtensionUpdateInfo.VersionUpdateType.ADD_VERSION) ? "+" : "-",
                                    e.getCurrentDep().getArtifact().getKey().toGacString()) + ":["
                                    + MessageFormatter.red("managed") + " -> " + MessageFormatter.green(
                                            e.getRecommendedDependency().getVersion())
                                    + "]");
                            break;
                        case UPDATE_VERSION:
                            log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "~",
                                    updateInfo(e)));
                            break;
                    }
                }
            }
            log.info("");
        }
        final List<ExtensionUpdateInfo> simpleVersionUpdates = extensionsUpdateInfo.getSimpleVersionUpdates().stream()
                .filter(u -> !u.getCurrentDep().isPlatformExtension()).toList();
        if (!simpleVersionUpdates.isEmpty()) {
            log.info(MessageFormatter.bold("Suggested extension updates from other origins:"));
            for (ExtensionUpdateInfo u : simpleVersionUpdates) {
                if (u.getVersionUpdateType() == ExtensionUpdateInfo.VersionUpdateType.PLATFORM_MANAGED) {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "-",
                            u.getCurrentDep().getArtifact().getKey().toGacString()) + ":["
                            + MessageFormatter.red(u.getCurrentDep().getVersion()) + "]");
                } else {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "~",
                            updateInfo(u)));
                }
            }
        }

        printSeparator(log);
        return true;
    }

    private static String updateInfo(ExtensionUpdateInfo u) {
        return updateInfo(u.getCurrentDep().getArtifact(), u.getRecommendedDependency().getVersion());
    }

    static void printSeparator(MessageWriter log) {
        log.info("");
        log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        log.info("");
    }

    static String updateInfo(ArtifactCoords current, String newVersion) {
        return current.getKey().toGacString() + ":[" + MessageFormatter.red(
                current.getVersion()) + " -> "
                + MessageFormatter.green(newVersion) + "]";
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
