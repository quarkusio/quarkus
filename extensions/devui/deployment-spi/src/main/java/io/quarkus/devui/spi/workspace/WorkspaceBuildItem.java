package io.quarkus.devui.spi.workspace;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * This hold all files in the user's project root
 */
public final class WorkspaceBuildItem extends SimpleBuildItem {
    private final WorkspaceItems workspaceItems;
    private final Path rootPath;

    public WorkspaceBuildItem(Path rootPath, List<WorkspaceItem> workspaceItems) {
        this.rootPath = rootPath;
        this.workspaceItems = new WorkspaceItems(workspaceItems, File.separator);
    }

    public WorkspaceItems getWorkspaceItems() {
        return workspaceItems;
    }

    public List<Path> getPaths() {
        return workspaceItems.items.stream()
                .map(WorkspaceBuildItem.WorkspaceItem::path)
                .collect(Collectors.toList());
    }

    public Path getRootPath() {
        return this.rootPath;
    }

    public record WorkspaceItems(List<WorkspaceItem> items, String fileSeparator) {
    }

    public record WorkspaceItem(String name, Path path) {

    }
}
