package io.quarkus.devui.spi.buildtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;

/**
 * Holds any Build time actions for Dev UI the extension has
 */
public final class BuildTimeActionBuildItem extends AbstractDevUIBuildItem {

    private final List<BuildTimeAction> actions = new ArrayList<>();

    public BuildTimeActionBuildItem() {
        super();
    }

    public BuildTimeActionBuildItem(String customIdentifier) {
        super(customIdentifier);
    }

    public void addAction(BuildTimeAction buildTimeAction) {
        this.actions.add(buildTimeAction);
    }

    public <T> void addAction(String methodName,
            Function<Map<String, String>, T> action) {
        this.addAction(new BuildTimeAction(methodName, action));
    }

    public List<BuildTimeAction> getActions() {
        return actions;
    }
}
