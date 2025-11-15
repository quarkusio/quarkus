package io.quarkus.vertx.http.deployment;

import java.util.Optional;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.http.deployment.HttpSecurityProcessor.IsApplicationBasicAuthRequired;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementSecurityRecorder;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.AuthenticationHandler;
import io.quarkus.vertx.http.runtime.security.ManagementInterfaceHttpAuthorizer;
import io.quarkus.vertx.http.runtime.security.ManagementPathMatchingHttpSecurityPolicy;

public class ManagementInterfaceSecurityProcessor {

    @BuildStep(onlyIfNot = IsApplicationBasicAuthRequired.class)
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem initBasicAuth(
            ManagementSecurityRecorder recorder,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig) {
        if (managementBuildTimeConfig.auth().basic().orElse(false)) {
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
            ManagementSecurityRecorder recorder,
            BuildProducer<ManagementInterfaceFilterBuildItem> filterBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> beanProducer,
            Optional<ManagementAuthenticationHandlerBuildItem> managementAuthenticationHandlerBuildItem) {
        if (managementAuthenticationHandlerBuildItem.isPresent()) {
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable()
                            .addBeanClass(HttpAuthenticator.class)
                            .addBeanClass(ManagementPathMatchingHttpSecurityPolicy.class)
                            .addBeanClass(ManagementInterfaceHttpAuthorizer.class).build());
            filterBuildItemBuildProducer
                    .produce(new ManagementInterfaceFilterBuildItem(
                            recorder.getAuthenticationHandler(managementAuthenticationHandlerBuildItem.get().handler),
                            ManagementInterfaceFilterBuildItem.AUTHENTICATION));
            filterBuildItemBuildProducer
                    .produce(new ManagementInterfaceFilterBuildItem(recorder.permissionCheckHandler(),
                            ManagementInterfaceFilterBuildItem.AUTHORIZATION));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void createManagementAuthMechHandler(
            ManagementSecurityRecorder recorder, Capabilities capabilities,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            BuildProducer<ManagementAuthenticationHandlerBuildItem> managementAuthMechHandlerProducer) {
        if (managementBuildTimeConfig.auth().enabled() && capabilities.isPresent(Capability.SECURITY)) {
            managementAuthMechHandlerProducer.produce(new ManagementAuthenticationHandlerBuildItem(
                    recorder.managementAuthenticationHandler(managementBuildTimeConfig.auth().proactive())));
        }
    }

    @Produce(PreRouterFinalizationBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void initializeAuthMechanismHandler(Optional<ManagementAuthenticationHandlerBuildItem> managementAuthenticationHandler,
            ManagementSecurityRecorder recorder, BeanContainerBuildItem containerBuildItem) {
        if (managementAuthenticationHandler.isPresent()) {
            recorder.initializeAuthenticationHandler(managementAuthenticationHandler.get().handler,
                    containerBuildItem.getValue());
        }
    }

    static final class ManagementAuthenticationHandlerBuildItem extends SimpleBuildItem {
        private final RuntimeValue<AuthenticationHandler> handler;

        private ManagementAuthenticationHandlerBuildItem(RuntimeValue<AuthenticationHandler> handler) {
            this.handler = handler;
        }
    }

}
