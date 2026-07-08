package io.quarkus.vertx.http.deployment;

import java.util.Optional;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.http.deployment.HttpSecurityProcessor.IsApplicationBasicAuthRequired;
import io.quarkus.vertx.http.runtime.RouteHandler;
import io.quarkus.vertx.http.runtime.management.ManagementConfig;
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
    SyntheticBeanBuildItem initBasicAuth(
            ServiceRegistrar reg,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig) {
        if (managementBuildTimeConfig.auth().basic().orElse(false)) {
            reg
                    .forService(BasicAuthenticationMechanism.class, "management")
                    .atPhase(Phase.STATIC_INIT)
                    .onStart(ctx -> new BasicAuthenticationMechanism(null, false));
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(BasicAuthenticationMechanism.class)
                    .types(HttpAuthenticationMechanism.class)
                    .scope(Singleton.class)
                    .serviceValue(BasicAuthenticationMechanism.class, "management");
            return configurator.done();
        }

        return null;
    }

    @BuildStep
    void setupAuthenticationMechanisms(
            ServiceRegistrar reg,
            BuildProducer<ManagementInterfaceFilterBuildItem> filterBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> beanProducer,
            Optional<ManagementAuthenticationHandlerBuildItem> managementAuthenticationHandlerBuildItem) {
        if (managementAuthenticationHandlerBuildItem.isPresent()) {
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable()
                            .addBeanClass(HttpAuthenticator.class)
                            .addBeanClass(ManagementPathMatchingHttpSecurityPolicy.class)
                            .addBeanClass(ManagementInterfaceHttpAuthorizer.class).build());

            reg
                    .forService(RouteHandler.class, "io.quarkus.vertx.http.management.authentication-filter")
                    .atPhase(Phase.STATIC_INIT)
                    .require(AuthenticationHandler.class, "management")
                    .onStart((ctx, handler) -> handler::handle);
            RouteHandler authHandler = reg.getRecorderProxy(RouteHandler.class,
                    "io.quarkus.vertx.http.management.authentication-filter");
            filterBuildItemBuildProducer
                    .produce(new ManagementInterfaceFilterBuildItem(authHandler,
                            ManagementInterfaceFilterBuildItem.AUTHENTICATION));

            reg
                    .forService(RouteHandler.class, "io.quarkus.vertx.http.management.permission-check-filter")
                    .atPhase(Phase.STATIC_INIT)
                    .onStart(ctx -> ManagementSecurityRecorder.permissionCheckHandler()::handle);
            RouteHandler permHandler = reg.getRecorderProxy(RouteHandler.class,
                    "io.quarkus.vertx.http.management.permission-check-filter");
            filterBuildItemBuildProducer
                    .produce(new ManagementInterfaceFilterBuildItem(permHandler,
                            ManagementInterfaceFilterBuildItem.AUTHORIZATION));
        }
    }

    @BuildStep
    void createManagementAuthMechHandler(
            ServiceRegistrar reg, Capabilities capabilities,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            BuildProducer<ManagementAuthenticationHandlerBuildItem> managementAuthMechHandlerProducer) {
        if (managementBuildTimeConfig.auth().enabled() && capabilities.isPresent(Capability.SECURITY)) {
            boolean proactive = managementBuildTimeConfig.auth().proactive();
            reg
                    .forService(AuthenticationHandler.class, "management")
                    .atPhase(Phase.STATIC_INIT)
                    .onStart(ctx -> new AuthenticationHandler(proactive));
            managementAuthMechHandlerProducer.produce(new ManagementAuthenticationHandlerBuildItem(
                    reg.staticInitServiceAsRuntimeValue(AuthenticationHandler.class, "management")));
        }
    }

    @Produce(PreRouterFinalizationBuildItem.class)
    @BuildStep
    void initializeAuthMechanismHandler(Optional<ManagementAuthenticationHandlerBuildItem> managementAuthenticationHandler,
            ServiceRegistrar reg, BeanContainerBuildItem containerBuildItem) {
        if (managementAuthenticationHandler.isPresent()) {
            reg
                    .forService("io.quarkus.vertx.http.management.init-auth-handler")
                    .require(AuthenticationHandler.class, "management")
                    .require(BeanContainer.class)
                    .require(ManagementConfig.class)
                    .onStart((ctx, handler, beanContainer, config) -> ManagementSecurityRecorder
                            .initializeAuthenticationHandler(handler, beanContainer, config));
        }
    }

    static final class ManagementAuthenticationHandlerBuildItem extends SimpleBuildItem {
        private final RuntimeValue<AuthenticationHandler> handler;

        private ManagementAuthenticationHandlerBuildItem(RuntimeValue<AuthenticationHandler> handler) {
            this.handler = handler;
        }
    }

}
