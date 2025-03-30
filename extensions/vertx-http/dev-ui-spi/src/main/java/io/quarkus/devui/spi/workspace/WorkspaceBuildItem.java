package io.quarkus.devui.spi.workspace;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * This hold all files in the user's project root
 */
public final class WorkspaceBuildItem extends SimpleBuildItem {
    private final List<WorkspaceItem> workspaceItems;

    public WorkspaceBuildItem(List<WorkspaceItem> workspaceItems) {
        this.workspaceItems = workspaceItems;
    }

    public List<WorkspaceItem> getWorkspaceItems() {
        return workspaceItems;
    }

    public List<Path> getPaths() {
        return workspaceItems.stream()
                .map(WorkspaceBuildItem.WorkspaceItem::path)
                .collect(Collectors.toList());
    }

    public static record WorkspaceItem(String name, Path path) {

    }
}
