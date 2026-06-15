package io.quarkus.aesh.deployment;

import java.util.Objects;

import io.quarkus.aesh.runtime.AeshMode;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Represents the resolved CLI execution mode determined during build time.
 * <p>
 * The mode is always resolved to either {@link AeshMode#console} or {@link AeshMode#runtime},
 * never {@link AeshMode#auto}. When the user selects {@code auto}, the mode is resolved
 * based on the discovered command annotations.
 */
public final class AeshModeBuildItem extends SimpleBuildItem {

    private final AeshMode resolvedMode;
    private final boolean hasUserDefinedMain;

    /**
     * @param resolvedMode the resolved execution mode, must not be {@code null}
     * @param hasUserDefinedMain whether the user has defined their own {@code @QuarkusMain}
     */
    public AeshModeBuildItem(AeshMode resolvedMode, boolean hasUserDefinedMain) {
        this.resolvedMode = Objects.requireNonNull(resolvedMode, "resolvedMode must not be null");
        this.hasUserDefinedMain = hasUserDefinedMain;
    }

    /**
     * The resolved execution mode (never {@link AeshMode#auto}).
     */
    public AeshMode getResolvedMode() {
        return resolvedMode;
    }

    /**
     * Whether the user has defined their own {@code @QuarkusMain} application class.
     * When true, the extension does not register its own {@code QuarkusApplication}.
     */
    public boolean hasUserDefinedMain() {
        return hasUserDefinedMain;
    }
}
