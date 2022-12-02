
package org.acme.quarkus.ext.h.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class AcmeQuarkusExtProcessor {

    private static final String FEATURE = "acme-quarkus-ext-h";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

}