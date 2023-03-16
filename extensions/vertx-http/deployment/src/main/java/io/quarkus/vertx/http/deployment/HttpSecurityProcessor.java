package io.quarkus.vertx.http.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.security.AuthenticatedHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.DenySecurityPolicy;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.HttpAuthorizer;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder;
import io.quarkus.vertx.http.runtime.security.MtlsAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.PathMatchingHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.PermitSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.SupplierImpl;
import io.vertx.core.http.ClientAuth;

public class HttpSecurityProcessor {

    @BuildStep
    public void builtins(BuildProducer<HttpSecurityPolicyBuildItem> producer, HttpBuildTimeConfig buildTimeConfig,
            BuildProducer<AdditionalBeanBuildItem> beanProducer) {
        producer.produce(new HttpSecurityPolicyBuildItem("deny", new SupplierImpl<>(new DenySecurityPolicy())));
        producer.produce(new HttpSecurityPolicyBuildItem("permit", new SupplierImpl<>(new PermitSecurityPolicy())));
        producer.produce(
                new HttpSecurityPolicyBuildItem("authenticated", new SupplierImpl<>(new AuthenticatedHttpSecurityPolicy())));

        beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(PathMatchingHttpSecurityPolicy.class));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem initFormAuth(
            HttpSecurityRecorder recorder,
            HttpBuildTimeConfig buildTimeConfig,
            BuildProducer<RouteBuildItem> filterBuildItemBuildProducer) {
        if (buildTimeConfig.auth.form) {
            if (!buildTimeConfig.auth.proactive) {
                filterBuildItemBuildProducer.produce(RouteBuildItem.builder().route(recorder.getFormPostLocation())
                        .handler(recorder.formAuthPostHandler()).build());
            }
            return SyntheticBeanBuildItem.configure(FormAuthenticationMechanism.class)
                    .types(HttpAuthenticationMechanism.class)
                    .setRuntimeInit()
                    .scope(Singleton.class)
                    .supplier(recorder.setupFormAuth()).done();
        }
        return null;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem initMtlsClientAuth(
            HttpSecurityRecorder recorder,
            HttpBuildTimeConfig buildTimeConfig) {
        if (isMtlsClientAuthenticationEnabled(buildTimeConfig)) {
            return SyntheticBeanBuildItem.configure(MtlsAuthenticationMechanism.class)
                    .types(HttpAuthenticationMechanism.class)
                    .setRuntimeInit()
                    .scope(Singleton.class)
                    .supplier(recorder.setupMtlsClientAuth()).done();
        }
        return null;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem initBasicAuth(
            HttpSecurityRecorder recorder,
            HttpBuildTimeConfig buildTimeConfig,
            BuildProducer<SecurityInformationBuildItem> securityInformationProducer) {
        //basic auth explicitly disabled
        if (buildTimeConfig.auth.basic.isPresent() && !buildTimeConfig.auth.basic.get()) {
            return null;
        }
        boolean basicExplicitlyEnabled = buildTimeConfig.auth.basic.orElse(false);
        if ((buildTimeConfig.auth.form || isMtlsClientAuthenticationEnabled(buildTimeConfig))
                && !basicExplicitlyEnabled) {
            //if form auth is enabled and we are not then we don't install
            return null;
        }
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(BasicAuthenticationMechanism.class)
                .types(HttpAuthenticationMechanism.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .supplier(recorder.setupBasicAuth(buildTimeConfig));
        if (!buildTimeConfig.auth.form && !isMtlsClientAuthenticationEnabled(buildTimeConfig)
                && !basicExplicitlyEnabled) {
            //if not explicitly enabled we make this a default bean, so it is the fallback if nothing else is defined
            configurator.defaultBean();
            securityInformationProducer.produce(SecurityInformationBuildItem.BASIC());
        }

        return configurator.done();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void initPermissions(HttpSecurityRecorder recorder,
            Capabilities capabilities,
            List<HttpSecurityPolicyBuildItem> httpSecurityPolicyBuildItemList) {
        if (capabilities.isPresent(Capability.SECURITY)) {
            Map<String, Supplier<HttpSecurityPolicy>> policyMap = new HashMap<>();
            for (HttpSecurityPolicyBuildItem e : httpSecurityPolicyBuildItemList) {
                if (policyMap.containsKey(e.getName())) {
                    throw new RuntimeException("Multiple HTTP security policies defined with name " + e.getName());
                }
                policyMap.put(e.getName(), e.policySupplier);
            }

            recorder.initPermissions(policyMap);
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupAuthenticationMechanisms(
            HttpSecurityRecorder recorder,
            BuildProducer<FilterBuildItem> filterBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> beanProducer,
            Capabilities capabilities,
            HttpBuildTimeConfig buildTimeConfig,
            BuildProducer<SecurityInformationBuildItem> securityInformationProducer) {

        if (!buildTimeConfig.auth.form && buildTimeConfig.auth.basic.orElse(false)) {
            securityInformationProducer.produce(SecurityInformationBuildItem.BASIC());
        }

        if (capabilities.isPresent(Capability.SECURITY)) {
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClass(HttpAuthenticator.class)
                            .addBeanClass(HttpAuthorizer.class).build());
            filterBuildItemBuildProducer
                    .produce(new FilterBuildItem(
                            recorder.authenticationMechanismHandler(buildTimeConfig.auth.proactive),
                            FilterBuildItem.AUTHENTICATION));
            filterBuildItemBuildProducer
                    .produce(new FilterBuildItem(recorder.permissionCheckHandler(), FilterBuildItem.AUTHORIZATION));
        }
    }

    private boolean isMtlsClientAuthenticationEnabled(HttpBuildTimeConfig buildTimeConfig) {
        return !ClientAuth.NONE.equals(buildTimeConfig.tlsClientAuth);
    }
}
