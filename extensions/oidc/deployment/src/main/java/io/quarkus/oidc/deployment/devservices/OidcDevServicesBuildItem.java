package io.quarkus.oidc.deployment.devservices;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item which indicates that Dev Services for OIDC are provided by another extension.
 * Dev Services for Keycloak will be disabled if this item is detected.
 */
public class OidcDevServicesBuildItem extends SimpleBuildItem {

}
