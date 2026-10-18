package io.quarkus.devservices.oidc;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;

/**
 * Marker used by Build Steps that perform tasks which must run after OIDC Dev Services was prepared
 * (the {@link DevServicesResultBuildItem} was created). If the OIDC Dev Service did not start, this item
 * won't be produced.
 */
public final class OidcDevServicesPreparedBuildItem extends SimpleBuildItem {

    OidcDevServicesPreparedBuildItem() {
    }

}
