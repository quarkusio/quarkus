package io.quarkus.vertx.http.deployment;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;

/**
 * This is a marker that indicates that the body handler should be installed
 * on all routes, as an extension requires the request to be fully buffered.
 */
public final class RequireBodyHandlerBuildItem extends MultiBuildItem {

    private final BooleanSupplier bodyHandlerRequiredCondition;

    /**
     * Creates {@link RequireBodyHandlerBuildItem} that requires body handler unconditionally installed on all routes.
     */
    public RequireBodyHandlerBuildItem() {
        bodyHandlerRequiredCondition = null;
    }

    /**
     * Creates {@link RequireBodyHandlerBuildItem} that requires body handler installed on all routes if the supplier returns
     * true.
     *
     * @param bodyHandlerRequiredCondition supplier that returns true at runtime if the body handler should be created
     */
    public RequireBodyHandlerBuildItem(BooleanSupplier bodyHandlerRequiredCondition) {
        this.bodyHandlerRequiredCondition = bodyHandlerRequiredCondition;
    }

    private BooleanSupplier getBodyHandlerRequiredCondition() {
        return bodyHandlerRequiredCondition;
    }

    public static BooleanSupplier[] getBodyHandlerRequiredConditions(List<RequireBodyHandlerBuildItem> bodyHandlerBuildItems) {
        if (bodyHandlerBuildItems.isEmpty()) {
            return new BooleanSupplier[] {};
        }
        BooleanSupplier[] customRuntimeConditions = bodyHandlerBuildItems
                .stream()
                .map(RequireBodyHandlerBuildItem::getBodyHandlerRequiredCondition)
                .filter(Objects::nonNull)
                .toArray(BooleanSupplier[]::new);
        if (customRuntimeConditions.length == bodyHandlerBuildItems.size()) {
            return customRuntimeConditions;
        }
        // at least one item requires body handler unconditionally
        return new BooleanSupplier[] { new VertxHttpRecorder.AlwaysCreateBodyHandlerSupplier() };
    }
}
