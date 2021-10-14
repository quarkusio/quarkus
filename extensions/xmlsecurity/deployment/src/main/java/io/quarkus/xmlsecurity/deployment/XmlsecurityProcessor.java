package io.quarkus.xmlsecurity.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class XmlsecurityProcessor {

    private static final String FEATURE = "xmlsecurity";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
