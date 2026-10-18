package io.quarkus.devui.spi.workspace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import io.quarkus.devjsonrpc.spi.AbstractDevBuildItem;

/**
 * Add an action to the Dev UI Workspace.
 */
public final class WorkspaceActionBuildItem extends AbstractDevBuildItem {
    private final List<ActionBuilder> actionBuilders = new ArrayList<>();

    public WorkspaceActionBuildItem() {
        super();
    }

    public WorkspaceActionBuildItem(ActionBuilder... actionBuilder) {
        super();
        this.actionBuilders.addAll(Arrays.asList(actionBuilder));
    }

    public WorkspaceActionBuildItem(String customIdentifier, ActionBuilder... actionBuilder) {
        super(customIdentifier);
        this.actionBuilders.addAll(Arrays.asList(actionBuilder));
    }

    public void addAction(ActionBuilder action) {
        this.actionBuilders.add(action);
    }

    public List<ActionBuilder> getActions() {
        this.actionBuilders.sort(Comparator.comparing(ActionBuilder::getLabel, String.CASE_INSENSITIVE_ORDER));
        return this.actionBuilders;
    }
}
