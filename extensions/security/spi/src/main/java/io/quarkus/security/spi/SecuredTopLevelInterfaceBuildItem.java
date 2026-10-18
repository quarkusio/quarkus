package io.quarkus.security.spi;

import java.util.Objects;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Enables Quarkus core extensions to include all the sub-interfaces for security annotation scanning.
 * If the sub-interfaces are implemented as CDI beans, we will detect and apply security annotations of the sub-interfaces
 * to their implementations. Support for endpoints and static methods may or may not require additional work depending
 * on the endpoint implementation (WS Next, REST, gRPC, ...) and is not guaranteed by this build item.
 *
 * @see SecuredInterfaceAnnotationBuildItem for more information
 */
public final class SecuredTopLevelInterfaceBuildItem extends MultiBuildItem {

    private final DotName topLevelInterface;

    public SecuredTopLevelInterfaceBuildItem(DotName topLevelInterface) {
        this.topLevelInterface = Objects.requireNonNull(topLevelInterface);
    }

    public DotName getTopLevelInterfaceName() {
        return topLevelInterface;
    }
}
