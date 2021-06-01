
package org.acme.quarkus.ext.d.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class AcmeQuarkusExtProcessor {

    private static final String FEATURE = "acme-quarkus-ext-d";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

}