package io.quarkus.vertx.http.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.DefaultHttpPermissionChecker;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.HttpAuthorizer;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder;

public class HttpSecurityProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupAuthenticationMechanisms(
            HttpSecurityRecorder recorder,
            BuildProducer<FilterBuildItem> filterBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> beanProducer,
            Capabilities capabilities,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListenerBuildItemBuildProducer,
            HttpBuildTimeConfig buildTimeConfig) {

        if (buildTimeConfig.auth.basic) {
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(BasicAuthenticationMechanism.class));
        }

        if (capabilities.isCapabilityPresent(Capabilities.SECURITY)) {
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClass(HttpAuthenticator.class)
                            .addBeanClass(HttpAuthorizer.class).build());
            filterBuildItemBuildProducer.produce(new FilterBuildItem(recorder.authenticationMechanismHandler(), 200));
            filterBuildItemBuildProducer.produce(new FilterBuildItem(recorder.permissionCheckHandler(), 100));

            if (!buildTimeConfig.auth.permissions.isEmpty()) {
                beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(DefaultHttpPermissionChecker.class));
                beanContainerListenerBuildItemBuildProducer
                        .produce(new BeanContainerListenerBuildItem(recorder.initPermissions(buildTimeConfig)));
            }
        } else {
            if (!buildTimeConfig.auth.permissions.isEmpty()) {
                throw new IllegalStateException("HTTP permissions have been set however security is not enabled");
            }
        }
    }
}
