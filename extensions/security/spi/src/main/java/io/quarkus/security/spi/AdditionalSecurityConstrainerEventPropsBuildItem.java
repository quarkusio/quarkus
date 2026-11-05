package io.quarkus.security.spi;

import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * This item allows to enhance properties of security events produced by SecurityConstrainer.
 * The SecurityConstrainer is usually invoked when CDI request context is already fully setup, and the additional
 * properties can be added based on the active context.
 * <p>
 * The supplier is captured by a service action of the Quarkus Security extension, therefore it must be a value
 * the service system is able to capture, for example a record whose components are capturable as well.
 * It must not be a value returned by a bytecode recorder.
 */
public final class AdditionalSecurityConstrainerEventPropsBuildItem extends SimpleBuildItem {

    private final Supplier<Map<String, Object>> additionalEventPropsSupplier;

    public AdditionalSecurityConstrainerEventPropsBuildItem(Supplier<Map<String, Object>> additionalEventPropsSupplier) {
        this.additionalEventPropsSupplier = additionalEventPropsSupplier;
    }

    public Supplier<Map<String, Object>> getAdditionalEventPropsSupplier() {
        return additionalEventPropsSupplier;
    }
}
