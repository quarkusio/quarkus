package io.quarkus.devui.spi.buildtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Holds any Build time actions for Dev UI the extension has
 */
public final class BuildTimeActionBuildItem extends AbstractDevUIBuildItem {

    private final List<BuildTimeAction> actions = new ArrayList<>();
    private final List<BuildTimeAction> subscriptions = new ArrayList<>();

    public BuildTimeActionBuildItem() {
        super();
    }

    public BuildTimeActionBuildItem(String customIdentifier) {
        super(customIdentifier);
    }

    private void addAction(BuildTimeAction buildTimeAction) {
        this.actions.add(buildTimeAction);
    }

    public <T> void addAction(String methodName,
            Function<Map<String, String>, T> action) {
        this.addAction(new BuildTimeAction(methodName, action));
    }

    public <T> void addAssistantAction(String methodName,
            BiFunction<Object, Map<String, String>, T> action) {
        this.addAction(new BuildTimeAction(methodName, action));
    }

    public <T> void addAction(String methodName,
            RuntimeValue runtimeValue) {
        this.addAction(new BuildTimeAction(methodName, runtimeValue));
    }

    public List<BuildTimeAction> getActions() {
        return actions;
    }

    public void addSubscription(BuildTimeAction buildTimeAction) {
        this.subscriptions.add(buildTimeAction);
    }

    public <T> void addSubscription(String methodName,
            Function<Map<String, String>, T> action) {
        this.addSubscription(new BuildTimeAction(methodName, action));
    }

    public <T> void addSubscription(String methodName,
            RuntimeValue runtimeValue) {
        this.addSubscription(new BuildTimeAction(methodName, runtimeValue));
    }

    public List<BuildTimeAction> getSubscriptions() {
        return subscriptions;
    }
}
