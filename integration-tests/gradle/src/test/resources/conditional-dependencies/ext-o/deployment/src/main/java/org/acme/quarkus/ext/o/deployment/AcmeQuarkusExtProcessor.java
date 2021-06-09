
package org.acme.quarkus.ext.o.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class AcmeQuarkusExtProcessor {

    private static final String FEATURE = "acme-quarkus-ext-o";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

}