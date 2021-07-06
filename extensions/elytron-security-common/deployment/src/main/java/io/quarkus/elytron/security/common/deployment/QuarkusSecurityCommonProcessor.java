package io.quarkus.elytron.security.common.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.elytron.security.common.runtime.ElytronCommonRecorder;

public class QuarkusSecurityCommonProcessor {
    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitBcryptUtil() {
        // this holds a SecureRandom static var that needs to be initialised at run time
        return new RuntimeInitializedClassBuildItem(BcryptUtil.class.getName());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void registerPasswordProvider(ElytronCommonRecorder recorder) {
        recorder.registerPasswordProvider();
    }

    /**
     * Register the Elytron-provided password factory SPI implementation
     *
     * @param classes producer factory for ReflectiveClassBuildItems
     */
    @BuildStep
    void services(BuildProducer<ReflectiveClassBuildItem> classes) {
        String[] allClasses = {
                "org.wildfly.security.password.impl.PasswordFactorySpiImpl",
        };
        classes.produce(new ReflectiveClassBuildItem(true, false, allClasses));
    }
}
