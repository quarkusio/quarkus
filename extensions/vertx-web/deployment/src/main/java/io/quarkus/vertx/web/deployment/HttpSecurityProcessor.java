package io.quarkus.vertx.web.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.web.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.web.runtime.security.HttpSecurityRecorder;

public class HttpSecurityProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    io.quarkus.vertx.web.deployment.FilterBuildItem setupAuthenticationMechanisms(
            HttpSecurityRecorder recorder, BuildProducer<AdditionalBeanBuildItem> beanProducer, Capabilities capabilities) {
        if (capabilities.isCapabilityPresent(Capabilities.SECURITY)) {
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClass(HttpAuthenticator.class).build());
            return new FilterBuildItem(recorder.authenticationMechanismHandler());
        }
        return null;
    }
}
