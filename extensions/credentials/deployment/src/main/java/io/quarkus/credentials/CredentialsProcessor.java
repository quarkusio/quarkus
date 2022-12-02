package io.quarkus.credentials;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class CredentialsProcessor {

    @BuildStep
    UnremovableBeanBuildItem unremoveable() {
        return UnremovableBeanBuildItem.beanTypes(CredentialsProvider.class);
    }

}
