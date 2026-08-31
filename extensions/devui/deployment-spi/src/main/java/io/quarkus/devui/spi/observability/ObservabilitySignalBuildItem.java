package io.quarkus.devui.spi.observability;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produced by a backend extension (e.g. OpenTelemetry) to contribute a signal to the
 * Dev UI Observability section. The core Dev UI collects these into a single
 * "Observability" left-menu section that links to each signal's (unlisted) detail page.
 */
public final class ObservabilitySignalBuildItem extends MultiBuildItem {

    private final String key;
    private final String title;
    private final String icon;
    private final String pageId;
    private final String countJsonRpcMethod;

    /**
     * @param key unique signal key, e.g. "traces"
     * @param title display title, e.g. "Traces"
     * @param icon Dev UI icon name, e.g. "font-awesome-solid:diagram-project"
     * @param pageId the target Dev UI page id to route to, e.g. "quarkus-opentelemetry/traces"
     * @param countJsonRpcMethod name of a JSON-RPC method returning a live count (forward-looking;
     *        not yet rendered on the signal tile in the POC), may be null
     */
    public ObservabilitySignalBuildItem(String key, String title, String icon,
            String pageId, String countJsonRpcMethod) {
        this.key = key;
        this.title = title;
        this.icon = icon;
        this.pageId = pageId;
        this.countJsonRpcMethod = countJsonRpcMethod;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    public String getPageId() {
        return pageId;
    }

    public String getCountJsonRpcMethod() {
        return countJsonRpcMethod;
    }
}
