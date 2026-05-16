package io.quarkus.devservices.keycloak;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;

/**
 * Extensions should produce this build item if a DEV UI card with
 * the Keycloak Admin link should be created for the extension.
 */
public final class KeycloakAdminPageBuildItem extends MultiBuildItem {

    final CardPageBuildItem cardPage;
    private final String capability;

    /**
     * @param cardPage created inside extension that requires Keycloak Dev Service, this way, card page
     *        custom identifier deduced from a stacktrace walker will identify the extension correctly
     */
    public KeycloakAdminPageBuildItem(CardPageBuildItem cardPage) {
        this.cardPage = cardPage;
        this.capability = null;
    }

    public KeycloakAdminPageBuildItem(CardPageBuildItem cardPage, String capability) {
        this.cardPage = cardPage;
        this.capability = capability;
    }

    boolean belongsToCapability(String other) {
        return capability != null && capability.equals(other);
    }

}
