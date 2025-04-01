package io.quarkus.devui.deployment.menu;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
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
@BuildSteps(onlyIf = IsDevelopment.class)
public class WorkspaceProcessor {

    @BuildStep
    void locateWorkspaceItems(BuildSystemTargetBuildItem buildSystemTarget,
            BuildProducer<WorkspaceBuildItem> workspaceProducer,
            DevUIConfig devUIConfig) {

        Path outputDir = buildSystemTarget.getOutputDirectory();
        Path projectRoot = outputDir.getParent();

        List<WorkspaceBuildItem.WorkspaceItem> workspaceItems = new ArrayList<>();

        List<String> ignoreFolders = devUIConfig.workspace().ignoreFolders().orElse(new ArrayList<>());
        ignoreFolders.add("node_modules");

        final List<Pattern> ignoreFilePatterns = devUIConfig.workspace().ignoreFiles().orElse(List.of());

        try {
            Files.walkFileTree(projectRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (Files.isHidden(dir) || ignoreFolders.contains(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();
                    boolean shouldIgnore = Files.isHidden(file)
                            || file.startsWith(outputDir)
                            || ignoreFilePatterns.stream().anyMatch(p -> p.matcher(fileName).matches());

                    if (!shouldIgnore) {
                        String name = projectRoot.relativize(file).toString();
                        workspaceItems.add(new WorkspaceBuildItem.WorkspaceItem(name, file));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        sortWorkspaceItems(workspaceItems);
        workspaceProducer.produce(new WorkspaceBuildItem(workspaceItems));
    }

    @BuildStep
    InternalPageBuildItem createReportIssuePage() {
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
                    Map params = (Map) t;
                    return params.get("content"); // We just return the content, we will markup in the UI
                })
                .display(Display.split)
                .displayType(DisplayType.markdown)
                .namespace(NAMESPACE)
                .filter(Patterns.README_MD);

        workspaceActionProducer.produce(new WorkspaceActionBuildItem(actionBuilder));
    }

    @BuildStep
    void createBuildTimeActions(WorkspaceBuildItem workspaceBuildItem,
            List<WorkspaceActionBuildItem> workspaceActionBuildItems,
            BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer) {

        // Dev UI Build Time Actions
        BuildTimeActionBuildItem buildItemActions = new BuildTimeActionBuildItem(NAMESPACE);

        // Workspace Actions
        Map<String, Action> actionMap = workspaceActionBuildItems.stream()
                .flatMap(item -> item.getActions().stream())
                .map(ActionBuilder::build)
                .collect(Collectors.toMap(Action::getId, action -> action, (a, b) -> a));

        buildItemActions.addAction("getWorkspaceItems", (t) -> {
            return workspaceBuildItem.getWorkspaceItems();
        });

        buildItemActions.addAction("getWorkspaceActions", (t) -> {
            return actionMap.values().stream()
                    .map(action -> new WorkspaceAction(action.getId(), action.getLabel(), action.getFilter(),
                            action.getDisplay(), action.getDisplayType()))
                    .sorted(Comparator.comparing(WorkspaceAction::label))
                    .collect(Collectors.toList());
        });

        buildItemActions.addAction("executeAction", (t) -> {

            String actionId = t.get("actionId");

            if (actionId != null) {
                Path path = Path.of(URI.create(t.get("path")));
                Action actionToExecute = actionMap.get(actionId);
                Path convertedPath = (Path) actionToExecute.getPathConverter().apply(path);
                return new WorkspaceActionResult(convertedPath, actionToExecute.getFunction().apply(t));
            }
            return null;
        });

        buildItemActions.addAction("getWorkspaceItemContent", (Map<String, String> params) -> {
            if (params.containsKey("path")) {
                Path path = Paths.get(URI.create(params.get("path")));
                return readContents(path);
            }
            return null;
        });

        buildItemActions.addAction("saveWorkspaceItemContent", (Map<String, String> params) -> {
            if (params.containsKey("content")) {
                String content = params.get("content");
                Path path = Paths.get(URI.create(params.get("path")));
                writeContent(path, content);
                return new SavedResult(path, true, null);
            }
            return new SavedResult(null, false, "Invalid input");
        });

        buildTimeActionProducer.produce(buildItemActions);
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

    static record SavedResult(Path path, boolean success, String errorMessage) {
    }

    static record WorkspaceContent(String type, String content, boolean isBinary) {
    }

    static record WorkspaceAction(String id, String label, Optional<Pattern> pattern, Display display,
            DisplayType displayType) {
    }

    static record WorkspaceActionResult(Path path, Object result) {
    }

    private static final String NAMESPACE = "devui-workspace";
}
