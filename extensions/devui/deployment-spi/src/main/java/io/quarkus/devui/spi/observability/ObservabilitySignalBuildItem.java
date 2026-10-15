package io.quarkus.devui.spi.observability;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produced by a backend extension (e.g. OpenTelemetry) to contribute a signal to the
 * Dev UI Observability dashboard. The core Dev UI collects these into a single
 * "Observability" left-menu page on which the user composes a dashboard of cards.
 * <p>
 * A signal with a {@code pageId} is embeddable: the dashboard resolves that page from the
 * Dev UI state, dynamically imports its web component and renders it as a card, with a link
 * out to the full page. A signal without one contributes no card of its own; it merely tells
 * the dashboard that the feature is available (metrics, for instance, expands into one card
 * per meter instead).
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
     * @param pageId the Dev UI page id backing this signal, e.g. "quarkus-opentelemetry/traces";
     *        null when the signal has no page of its own to embed or link to
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
