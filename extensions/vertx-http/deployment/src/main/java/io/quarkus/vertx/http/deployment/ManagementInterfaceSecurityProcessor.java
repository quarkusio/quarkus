package io.quarkus.vertx.http.deployment;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.PolicyConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceSecurityRecorder;
import io.quarkus.vertx.http.runtime.security.AuthenticatedHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.DenySecurityPolicy;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.ManagementInterfaceHttpAuthorizer;
import io.quarkus.vertx.http.runtime.security.ManagementPathMatchingHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.PermitSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.RolesAllowedHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.SupplierImpl;

public class ManagementInterfaceSecurityProcessor {

    @BuildStep
    public void builtins(ManagementInterfaceBuildTimeConfig buildTimeConfig,
            BuildProducer<AdditionalBeanBuildItem> beanProducer) {
        if (!buildTimeConfig.auth.permissions.isEmpty()) {
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(ManagementPathMatchingHttpSecurityPolicy.class));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem initBasicAuth(
            HttpBuildTimeConfig httpBuildTimeConfig,
            ManagementInterfaceSecurityRecorder recorder,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig) {
        if (HttpSecurityProcessor.applicationBasicAuthRequired(httpBuildTimeConfig, managementInterfaceBuildTimeConfig)) {
            return null;
        }

        if (managementInterfaceBuildTimeConfig.auth.basic.orElse(false)) {
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(BasicAuthenticationMechanism.class)
                    .types(HttpAuthenticationMechanism.class)
                    .setRuntimeInit()
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
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListenerBuildItemBuildProducer,
            ManagementInterfaceBuildTimeConfig buildTimeConfig) {

        Map<String, Supplier<HttpSecurityPolicy>> policyMap = new HashMap<>();
        for (Map.Entry<String, PolicyConfig> e : buildTimeConfig.auth.rolePolicy.entrySet()) {
            policyMap.put(e.getKey(),
                    new SupplierImpl<>(new RolesAllowedHttpSecurityPolicy(e.getValue().rolesAllowed)));
        }
        policyMap.put("deny", new SupplierImpl<>(new DenySecurityPolicy()));
        policyMap.put("permit", new SupplierImpl<>(new PermitSecurityPolicy()));
        policyMap.put("authenticated", new SupplierImpl<>(new AuthenticatedHttpSecurityPolicy()));

        if (buildTimeConfig.auth.basic.orElse(false)
                && capabilities.isPresent(Capability.SECURITY)) {
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable()
                            .addBeanClass(HttpAuthenticator.class)
                            .addBeanClass(ManagementInterfaceHttpAuthorizer.class).build());
            filterBuildItemBuildProducer
                    .produce(new ManagementInterfaceFilterBuildItem(
                            recorder.authenticationMechanismHandler(buildTimeConfig.auth.proactive),
                            ManagementInterfaceFilterBuildItem.AUTHENTICATION));
            filterBuildItemBuildProducer
                    .produce(new ManagementInterfaceFilterBuildItem(recorder.permissionCheckHandler(buildTimeConfig, policyMap),
                            ManagementInterfaceFilterBuildItem.AUTHORIZATION));
            if (!buildTimeConfig.auth.permissions.isEmpty()) {
                beanContainerListenerBuildItemBuildProducer
                        .produce(new BeanContainerListenerBuildItem(recorder.initPermissions(buildTimeConfig, policyMap)));
            }
        } else {
            if (!buildTimeConfig.auth.permissions.isEmpty()) {
                throw new IllegalStateException("HTTP permissions have been set however security is not enabled");
            }
        }
    }
}
