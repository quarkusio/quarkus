package io.quarkus.deployment.sbom;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.sbom.SbomContribution;

/**
 * A build item that allows Quarkus extensions to contribute additional components
 * to the application SBOM.
 * <p>
 * This build item carries an {@link SbomContribution} containing component descriptors and
 * their dependency relationships from any package ecosystem (npm, Maven, etc.).
 * <p>
 * Extension contributions are merged with the core application contribution (assembled from the
 * application's Maven dependency model and packaging output) by an SBOM generator.
 * Extensions should not designate a main component — only the core contribution does that.
 * <p>
 * The API around this is still in development and will likely change in the near future.
 */
public final class SbomContributionBuildItem extends MultiBuildItem {

    private final SbomContribution contribution;

    /**
     * Creates an SBOM contribution build item with the specified contribution.
     *
     * @param contribution the SBOM contribution containing components and dependencies
     * @throws NullPointerException if contribution is null
     */
    public SbomContributionBuildItem(SbomContribution contribution) {
        this.contribution = Objects.requireNonNull(contribution, "contribution cannot be null");
    }

    /**
     * Gets the SBOM contribution carried by this build item.
     *
     * @return the SBOM contribution
     */
    public SbomContribution getContribution() {
        return contribution;
    }
}
