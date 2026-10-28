package io.quarkus.load.shedding.deployment.spi;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Declares a path that is never subject to load shedding, whether priority load shedding
 * is enabled or not.
 * <p>
 * This is meant for the operational control plane only, such as the health probes or the
 * metrics endpoint: an orchestrator that probes such an endpoint would otherwise conclude
 * that an overloaded application is dead and restart it. A request to such a path should
 * execute quickly, read pre-aggregated values from memory, and perform no I/O.
 */
public final class LoadSheddingExemptPathBuildItem extends MultiBuildItem {
    private final String path;
    private final String reason;

    /**
     * @param path the absolute path of the endpoint, as served by the main HTTP router
     * @param reason why the endpoint must keep responding while the application is overloaded;
     *        reported in the debug log so that an unexpected exemption can be traced to its origin
     */
    public LoadSheddingExemptPathBuildItem(String path, String reason) {
        this.path = Objects.requireNonNull(path, "path");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public String getPath() {
        return path;
    }

    public String getReason() {
        return reason;
    }
}
