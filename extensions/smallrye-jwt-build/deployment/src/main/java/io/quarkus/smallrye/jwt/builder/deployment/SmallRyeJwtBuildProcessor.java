package io.quarkus.smallrye.jwt.builder.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.impl.JwtProviderImpl;

class SmallRyeJwtBuildProcessor {

    @BuildStep
    void addClassesForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, SignatureAlgorithm.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, JwtProviderImpl.class));
    }
}
