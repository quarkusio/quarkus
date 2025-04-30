package io.quarkus.vertx.http.deployment;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.ErrorPageAction;

/**
 * Allows extensions to contribute an action (button) to the error page
 */
public final class ErrorPageActionsBuildItem extends MultiBuildItem {
    private final List<ErrorPageAction> actions;

    public ErrorPageActionsBuildItem(String name, String url) {
        this(new ErrorPageAction(name, url));
    }

    public ErrorPageActionsBuildItem(ErrorPageAction errorPageAction) {
        this(List.of(errorPageAction));
    }

    public ErrorPageActionsBuildItem(List<ErrorPageAction> errorPageAction) {
        this.actions = errorPageAction;
    }

    public List<ErrorPageAction> getActions() {
        return actions;
    }
}
