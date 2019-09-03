package io.quarkus.vertx.http.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder;

public class HttpSecurityProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    FilterBuildItem setupAuthenticationMechanisms(
            HttpSecurityRecorder recorder,
            BuildProducer<AdditionalBeanBuildItem> beanProducer,
            Capabilities capabilities,
            HttpAuthConfig authConfig) {
        if (capabilities.isCapabilityPresent(Capabilities.SECURITY)) {
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClass(HttpAuthenticator.class).build());
            return new FilterBuildItem(recorder.authenticationMechanismHandler());
        }
        if (authConfig.basic) {
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(BasicAuthenticationMechanism.class));
        }
        return null;
    }
}
