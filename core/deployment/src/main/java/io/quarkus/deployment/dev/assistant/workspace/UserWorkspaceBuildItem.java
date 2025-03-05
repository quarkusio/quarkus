package io.quarkus.deployment.dev.assistant.workspace;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * This hold all files in the user's project root
 * TODO: This is not AI specific as can be useful for other use-cases
 */
public final class UserWorkspaceBuildItem extends SimpleBuildItem {
    private final List<WorkspaceItem> workspaceItems;

    public UserWorkspaceBuildItem(List<WorkspaceItem> workspaceItems) {
        this.workspaceItems = workspaceItems;
    }

    public List<WorkspaceItem> getWorkspaceItems() {
        return workspaceItems;
    }

    public List<Path> getPaths() {
        return workspaceItems.stream()
                .map(UserWorkspaceBuildItem.WorkspaceItem::path)
                .collect(Collectors.toList());
    }

    public static record WorkspaceItem(String name, Path path) {

    }
}
