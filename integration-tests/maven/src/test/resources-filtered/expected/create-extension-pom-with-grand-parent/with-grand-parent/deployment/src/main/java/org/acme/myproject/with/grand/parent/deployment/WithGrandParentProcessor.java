package org.acme.myproject.with.grand.parent.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class WithGrandParentProcessor {

    // Custom

    private static final String FEATURE = "with-grand-parent";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

}
