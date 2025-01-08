package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class NioSocketImplProcessor {

    // Workaround till https://github.com/oracle/graal/pull/10431 gets merged and backported to all supported versions
    @BuildStep
    RuntimeInitializedClassBuildItem reinitializeClass() {
        return new RuntimeInitializedClassBuildItem("sun.nio.ch.NioSocketImpl");
    }

}
