package io.quarkus.vertx.http.deployment;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.deployment.HttpSecurityProcessor.IsApplicationBasicAuthRequired;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceSecurityRecorder;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.ManagementInterfaceHttpAuthorizer;
import io.quarkus.vertx.http.runtime.security.ManagementPathMatchingHttpSecurityPolicy;

public class ManagementInterfaceSecurityProcessor {

    @BuildStep(onlyIfNot = IsApplicationBasicAuthRequired.class)
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem initBasicAuth(
            ManagementInterfaceSecurityRecorder recorder,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig) {
        if (managementInterfaceBuildTimeConfig.auth.basic.orElse(false)) {
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(BasicAuthenticationMechanism.class)
                    .types(HttpAuthenticationMechanism.class)
                    .scope(Singleton.class)
                    .supplier(recorder.setupBasicAuth());
            return configurator.done();
        }

        return null;
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupAuthenticationMechanisms(
            ManagementInterfaceSecurityRecorder recorder,
            BuildProducer<ManagementInterfaceFilterBuildItem> filterBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> beanProducer,
            Capabilities capabilities,
            ManagementInterfaceBuildTimeConfig buildTimeConfig) {
        if (buildTimeConfig.auth.basic.orElse(false)
                && capabilities.isPresent(Capability.SECURITY)) {
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable()
                            .addBeanClass(HttpAuthenticator.class)
                            .addBeanClass(ManagementPathMatchingHttpSecurityPolicy.class)
                            .addBeanClass(ManagementInterfaceHttpAuthorizer.class).build());
            filterBuildItemBuildProducer
                    .produce(new ManagementInterfaceFilterBuildItem(
                            recorder.authenticationMechanismHandler(buildTimeConfig.auth.proactive),
                            ManagementInterfaceFilterBuildItem.AUTHENTICATION));
            filterBuildItemBuildProducer
                    .produce(new ManagementInterfaceFilterBuildItem(recorder.permissionCheckHandler(),
                            ManagementInterfaceFilterBuildItem.AUTHORIZATION));
        }
    }
}
