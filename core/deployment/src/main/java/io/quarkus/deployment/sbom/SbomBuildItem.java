package io.quarkus.deployment.sbom;

import java.util.Objects;

import io.quarkus.bootstrap.app.SbomResult;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Aggregates SBOMs generated for packaged applications.
 * The API around this is still in development and will likely change in the near future.
 */
public final class SbomBuildItem extends MultiBuildItem {

    private final SbomResult result;

    public SbomBuildItem(SbomResult result) {
        this.result = Objects.requireNonNull(result);
    }

    public SbomResult getResult() {
        return result;
    }
}
