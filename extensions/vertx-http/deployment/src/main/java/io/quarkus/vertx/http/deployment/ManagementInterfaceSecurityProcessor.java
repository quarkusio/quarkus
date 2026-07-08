package io.quarkus.vertx.http.deployment;

import java.util.Optional;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.http.deployment.HttpSecurityProcessor.IsApplicationBasicAuthRequired;
import io.quarkus.vertx.http.runtime.management.ManagementConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementSecurityRecorder;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.AuthenticationHandler;
import io.quarkus.vertx.http.runtime.security.ManagementInterfaceHttpAuthorizer;
import io.quarkus.vertx.http.runtime.security.ManagementPathMatchingHttpSecurityPolicy;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ManagementInterfaceSecurityProcessor {

    @BuildStep(onlyIfNot = IsApplicationBasicAuthRequired.class)
    SyntheticBeanBuildItem initBasicAuth(
            ActionBuilder action,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig) {
        if (managementBuildTimeConfig.auth().basic().orElse(false)) {
            action
                    .forService(BasicAuthenticationMechanism.class, "management")
                    .atPhase(Phase.STATIC_INIT)
                    .action(ctx -> new BasicAuthenticationMechanism(null, false));
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(BasicAuthenticationMechanism.class)
                    .types(HttpAuthenticationMechanism.class)
                    .scope(Singleton.class)
                    .serviceValue(BasicAuthenticationMechanism.class, "management");
            return configurator.done();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @BuildStep
    void setupAuthenticationMechanisms(
            ActionBuilder action,
            BuildProducer<ManagementInterfaceFilterBuildItem> filterBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> beanProducer,
            Optional<ManagementAuthenticationHandlerBuildItem> managementAuthenticationHandlerBuildItem) {
        if (managementAuthenticationHandlerBuildItem.isPresent()) {
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable()
                            .addBeanClass(HttpAuthenticator.class)
                            .addBeanClass(ManagementPathMatchingHttpSecurityPolicy.class)
                            .addBeanClass(ManagementInterfaceHttpAuthorizer.class).build());

            action
                    .forService(Handler.class, "io.quarkus.vertx.http.management.authentication-filter")
                    .atPhase(Phase.STATIC_INIT)
                    .require(AuthenticationHandler.class, "management")
                    .action((ctx, handler) -> handler);
            Handler<RoutingContext> authHandler = (Handler<RoutingContext>) action.staticInitServiceAsRecorderValue(
                    Handler.class, "io.quarkus.vertx.http.management.authentication-filter");
            filterBuildItemBuildProducer
                    .produce(new ManagementInterfaceFilterBuildItem(authHandler,
                            ManagementInterfaceFilterBuildItem.AUTHENTICATION));

            action
                    .forService(Handler.class, "io.quarkus.vertx.http.management.permission-check-filter")
                    .atPhase(Phase.STATIC_INIT)
                    .action(ctx -> ManagementSecurityRecorder.permissionCheckHandler());
            Handler<RoutingContext> permHandler = (Handler<RoutingContext>) action.staticInitServiceAsRecorderValue(
                    Handler.class, "io.quarkus.vertx.http.management.permission-check-filter");
            filterBuildItemBuildProducer
                    .produce(new ManagementInterfaceFilterBuildItem(permHandler,
                            ManagementInterfaceFilterBuildItem.AUTHORIZATION));
        }
    }

    @BuildStep
    void createManagementAuthMechHandler(
            ActionBuilder action, Capabilities capabilities,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            BuildProducer<ManagementAuthenticationHandlerBuildItem> managementAuthMechHandlerProducer) {
        if (managementBuildTimeConfig.auth().enabled() && capabilities.isPresent(Capability.SECURITY)) {
            boolean proactive = managementBuildTimeConfig.auth().proactive();
            action
                    .forService(AuthenticationHandler.class, "management")
                    .atPhase(Phase.STATIC_INIT)
                    .action(ctx -> new AuthenticationHandler(proactive));
            managementAuthMechHandlerProducer.produce(new ManagementAuthenticationHandlerBuildItem(
                    action.staticInitServiceAsRuntimeValue(AuthenticationHandler.class, "management")));
        }
    }

    @Produce(PreRouterFinalizationBuildItem.class)
    @BuildStep
    void initializeAuthMechanismHandler(Optional<ManagementAuthenticationHandlerBuildItem> managementAuthenticationHandler,
            ActionBuilder action, BeanContainerBuildItem containerBuildItem) {
        if (managementAuthenticationHandler.isPresent()) {
            action
                    .forService("io.quarkus.vertx.http.management.init-auth-handler")
                    .require(AuthenticationHandler.class, "management")
                    .require(BeanContainer.class)
                    .require(ManagementConfig.class)
                    .action((ctx, handler, beanContainer, config) -> ManagementSecurityRecorder
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
