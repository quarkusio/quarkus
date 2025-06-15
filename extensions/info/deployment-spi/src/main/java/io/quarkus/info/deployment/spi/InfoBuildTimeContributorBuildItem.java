package io.quarkus.info.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.info.runtime.spi.InfoContributor;

/**
 * Allows for extensions to include their own {@link InfoContributor} implementations that result in inclusion of
 * properties in the info endpoint
 */
public final class InfoBuildTimeContributorBuildItem extends MultiBuildItem {

    private final InfoContributor infoContributor;

    public InfoBuildTimeContributorBuildItem(InfoContributor infoContributor) {
        this.infoContributor = infoContributor;
    }

    public InfoContributor getInfoContributor() {
        return infoContributor;
    }
}
