package io.quarkus.devui.deployment.menu;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.assistant.runtime.dev.Assistant;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.deployment.DevUIConfig;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.workspace.Action;
import io.quarkus.devui.spi.workspace.ActionBuilder;
import io.quarkus.devui.spi.workspace.Display;
import io.quarkus.devui.spi.workspace.DisplayType;
import io.quarkus.devui.spi.workspace.Patterns;
import io.quarkus.devui.spi.workspace.WorkspaceActionBuildItem;
import io.quarkus.devui.spi.workspace.WorkspaceBuildItem;

/**
 * This creates the workspace Page
 */
@BuildSteps(onlyIf = IsLocalDevelopment.class)
public class WorkspaceProcessor {
    private static final Logger LOG = Logger.getLogger(WorkspaceProcessor.class);

    @BuildStep
    void locateWorkspaceItems(BuildSystemTargetBuildItem buildSystemTarget,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<WorkspaceBuildItem> workspaceProducer,
            DevUIConfig devUIConfig) {

        if (launchModeBuildItem.isNotLocalDevModeType()) {
            return;
        }

        Path outputDir = buildSystemTarget.getOutputDirectory();
        Path projectRoot = outputDir.getParent();
        if (projectRoot != null && Files.exists(projectRoot)) {

            List<WorkspaceBuildItem.WorkspaceItem> workspaceItems = new ArrayList<>();

            List<String> ignoreFolders = devUIConfig.workspace().ignoreFolders().orElse(new ArrayList<>());
            ignoreFolders.add("node_modules");

            final List<Pattern> ignoreFilePatterns = devUIConfig.workspace().ignoreFiles().orElse(List.of());

            try {
                Files.walkFileTree(projectRoot, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        try {
                            if (Files.isHidden(dir) || ignoreFolders.contains(dir.getFileName().toString())
                                    || !Files.isReadable(dir) || !Files.isExecutable(dir)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                        } catch (Throwable t) {
                            // Don't let this stop startup
                            LOG.debug("Error while getting workspace items", t);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        String fileName = file.getFileName().toString();
                        try {
                            boolean shouldIgnore = Files.isHidden(file)
                                    || file.startsWith(outputDir) || !Files.isReadable(file)
                                    || ignoreFilePatterns.stream().anyMatch(p -> p.matcher(fileName).matches());

                            if (!shouldIgnore) {
                                String name = projectRoot.relativize(file).toString();
                                workspaceItems.add(new WorkspaceBuildItem.WorkspaceItem(name, file));
                            }
                        } catch (Throwable t) {
                            // Don't let this stop startup
                            LOG.debug("Error while getting workspace items", t);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        if (exc instanceof AccessDeniedException) {
                            if (Files.isDirectory(file)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            } else {
                                return FileVisitResult.CONTINUE;
                            }
                        }
                        return super.visitFileFailed(file, exc);
                    }
                });

            } catch (IOException e) {
                // Don't let this stop startup
                LOG.debug("Error while getting workspace items", e);
            }

            sortWorkspaceItems(workspaceItems);
            workspaceProducer.produce(new WorkspaceBuildItem(projectRoot, workspaceItems));
        }
    }

    @BuildStep
    InternalPageBuildItem createWorkspacePage() {
        InternalPageBuildItem item = new InternalPageBuildItem("Workspace", 25);

        item.addPage(Page.webComponentPageBuilder().internal()
                .namespace(NAMESPACE)
                .title("Workspace")
                .icon("font-awesome-solid:folder-tree")
                .componentLink("qwc-workspace.js"));

        return item;
    }

    @BuildStep
    void createDefaultWorkspaceActions(BuildProducer<WorkspaceActionBuildItem> workspaceActionProducer) {
        ActionBuilder actionBuilder = Action.actionBuilder()
                .label("Preview")
                .function((t) -> {
                    return t; // We just return the content, we will markup in the UI
                })
                .display(Display.split)
                .displayType(DisplayType.markdown)
                .namespace(NAMESPACE)
                .filter(Patterns.ANY_MD);

        workspaceActionProducer.produce(new WorkspaceActionBuildItem(NAMESPACE, actionBuilder));
    }

    @BuildStep
    void createBuildTimeActions(Optional<WorkspaceBuildItem> workspaceBuildItem,
            List<WorkspaceActionBuildItem> workspaceActionBuildItems,
            BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer,
            Capabilities capabilities,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {

        final boolean assistantIsAvailable = capabilities.isPresent(Capability.ASSISTANT);

        if (workspaceBuildItem.isPresent()) {

            // Dev UI Build Time Actions
            BuildTimeActionBuildItem buildItemActions = new BuildTimeActionBuildItem(NAMESPACE);

            // Workspace Actions
            Map<String, Action> actionMap = workspaceActionBuildItems.stream()
                    .flatMap(item -> item.getActions().stream()
                            .map(builder -> builder
                                    .namespace(item.getExtensionPathName(curateOutcomeBuildItem))
                                    .build()))
                    .collect(Collectors.toMap(Action::getId, action -> action, (a, b) -> a));

            buildItemActions.actionBuilder()
                    .methodName("getWorkspaceItems")
                    .description(
                            "Gets all the items in the current workspace of the Quarkus project. This will return a list of workspace items, where a workspace item has a name and a path to the file")
                    .function((t) -> {
                        return workspaceBuildItem.get().getWorkspaceItems();
                    })
                    .enableMcpFuctionByDefault()
                    .build();

            buildItemActions.actionBuilder()
                    .methodName("getWorkspaceActions")
                    .description("Gets all the actions that can be performed on a item in the workspace")
                    .function((t) -> {
                        return actionMap.values().stream()
                                .map(action -> new WorkspaceAction(action.getId(), action.getLabel(), action.getFilter(),
                                        action.getDisplay(), action.getDisplayType(), action.isAssistant()))
                                .sorted(Comparator.comparing(WorkspaceAction::label))
                                .collect(Collectors.toList());
                    })
                    .build();

            buildItemActions.actionBuilder()
                    .methodName("executeAction")
                    .description("Execute a certain action on a workspace item")
                    .parameter("actionId",
                            "The actionId as defined in the `getWorkspaceActions`. Each workspace action has a unique id")
                    .parameter("path",
                            "The path, as a URI in String format, to the workspace item that this action should be peformed on")
                    .function((Map<String, String> t) -> {
                        String actionId = t.get("actionId");
                        if (actionId != null) {
                            Path path = Path.of(URI.create(t.get("path")));
                            Action actionToExecute = actionMap.get(actionId);
                            Path convertedPath = (Path) actionToExecute.getPathConverter().apply(path);

                            Object result;
                            if (actionToExecute.isAssistant()) {
                                Assistant assistant = DevConsoleManager
                                        .getGlobal(DevConsoleManager.DEV_MANAGER_GLOBALS_ASSISTANT);
                                result = actionToExecute.getAssistantFunction().apply(assistant, t);
                            } else {
                                result = actionToExecute.getFunction().apply(t);
                            }

                            if (result != null && result instanceof CompletionStage<?> stage) {
                                return stage
                                        .thenApply(res -> new WorkspaceActionResult(convertedPath, res,
                                                actionToExecute.isAssistant()));
                            } else {
                                return new WorkspaceActionResult(convertedPath, result, actionToExecute.isAssistant());
                            }
                        }
                        return null;
                    })
                    .build();

            buildItemActions.actionBuilder()
                    .methodName("getWorkspaceItemContent")
                    .description(
                            "The the content of a certain workspace item. This returns a WorkspaceContent that has String type, String content, boolean isBinary fields")
                    .parameter("path", "The path, as a URI in String format, to the workspace item that content is requested")
                    .function((Map<String, String> params) -> {
                        if (params.containsKey("path")) {
                            Path path = Paths.get(URI.create(params.get("path")));
                            return readContents(path);
                        }
                        return null;
                    })
                    .enableMcpFuctionByDefault()
                    .build();

            buildItemActions.actionBuilder()
                    .methodName("saveWorkspaceItemContent")
                    .description("This will add or update an item in the workspace")
                    .parameter("content", "The new or updated content in String format")
                    .parameter("path",
                            "The path, as a URI in String format, to the workspace item that should be created or updated")
                    .function((Map<String, String> params) -> {
                        if (params.containsKey("content")) {
                            String content = params.get("content");
                            Path path = Paths.get(URI.create(params.get("path")));
                            writeContent(path, content);
                            return new SavedResult(workspaceBuildItem.get().getRootPath().relativize(path).toString(), true,
                                    null);
                        }
                        return new SavedResult(null, false, "Invalid input");
                    })
                    .enableMcpFuctionByDefault()
                    .build();

            buildTimeActionProducer.produce(buildItemActions);
        }
    }

    private void sortWorkspaceItems(List<WorkspaceBuildItem.WorkspaceItem> items) {
        items.sort(Comparator.comparing((WorkspaceBuildItem.WorkspaceItem item) -> isFileInRoot(item.name()))
                .thenComparing(item -> folderPriority(item.name()))
                .thenComparing(WorkspaceBuildItem.WorkspaceItem::name));
    }

    private static int folderPriority(String name) {
        if (name.startsWith("src/main/java"))
            return 1;
        if (name.startsWith("src/main/resources"))
            return 2;
        if (name.startsWith("src/main/"))
            return 3;
        if (name.startsWith("src/"))
            return 4;

        if (name.startsWith("src/test/java"))
            return 5;
        if (name.startsWith("src/test/resources"))
            return 6;
        if (name.startsWith("src/test/"))
            return 7;

        if (name.startsWith("src/integrationTest/java"))
            return 8;
        if (name.startsWith("src/integrationTest/resources"))
            return 9;
        if (name.startsWith("src/integrationTest/"))
            return 10;

        return 11;
    }

    private boolean isFileInRoot(String name) {
        return !name.contains("/");
    }

    private String writeContent(Path path, String contents) {
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path))
                Files.createFile(path);
            Files.writeString(path, contents, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE);
            return path.toString();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private WorkspaceContent readContents(Path filePath) {
        if (filePath != null && Files.exists(filePath) && Files.isReadable(filePath)) {
            String mimeType = getMimeType(filePath);
            try {
                return new WorkspaceContent(mimeType, Files.readString(filePath), false);
            } catch (IOException ex) {
                return getEncodedBytes(mimeType, filePath);
            }
        }

        return null;
    }

    private String getMimeType(Path filePath) {
        try {
            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) {
                return getFileTypeManually(filePath);
            }
            return mimeType;
        } catch (IOException ex) {
            return getFileTypeManually(filePath);
        }
    }

    private WorkspaceContent getEncodedBytes(String mimeType, Path filePath) {
        try {
            return new WorkspaceContent(mimeType, Base64.getEncoder().encodeToString(Files.readAllBytes(filePath)), true);
        } catch (IOException ex) {
            return new WorkspaceContent("text",
                    "Error: Could not read content of " + mimeType + " item. \n [" + ex.getMessage() + "]", false);
        }
    }

    private String getFileTypeManually(Path filePath) {
        String fileName = filePath.getFileName().toString();
        return fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "";
    }

    static record SavedResult(String path, boolean success, String errorMessage) {
    }

    static record WorkspaceContent(String type, String content, boolean isBinary) {
    }

    static record WorkspaceAction(String id, String label, Optional<Pattern> pattern, Display display,
            DisplayType displayType, boolean isAssistanceAction) {
    }

    static record WorkspaceActionResult(Path path, Object result, boolean isAssistant) {
    }

    private static final String NAMESPACE = "devui-workspace";
}
