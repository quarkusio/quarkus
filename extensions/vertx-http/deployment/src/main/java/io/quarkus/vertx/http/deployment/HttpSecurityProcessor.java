package io.quarkus.vertx.http.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.PolicyConfig;
import io.quarkus.vertx.http.runtime.security.AuthenticatedHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.DenySecurityPolicy;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.HttpAuthorizer;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder;
import io.quarkus.vertx.http.runtime.security.PathMatchingHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.PermitSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.RolesAllowedHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.SupplierImpl;

public class HttpSecurityProcessor {

    @BuildStep
    public void builtins(BuildProducer<HttpSecurityPolicyBuildItem> producer, HttpBuildTimeConfig buildTimeConfig,
            BuildProducer<AdditionalBeanBuildItem> beanProducer) {
        producer.produce(new HttpSecurityPolicyBuildItem("deny", new SupplierImpl<>(new DenySecurityPolicy())));
        producer.produce(new HttpSecurityPolicyBuildItem("permit", new SupplierImpl<>(new PermitSecurityPolicy())));
        producer.produce(
                new HttpSecurityPolicyBuildItem("authenticated", new SupplierImpl<>(new AuthenticatedHttpSecurityPolicy())));
        if (!buildTimeConfig.auth.permissions.isEmpty()) {
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(PathMatchingHttpSecurityPolicy.class));
        }
        for (Map.Entry<String, PolicyConfig> e : buildTimeConfig.auth.rolePolicy.entrySet()) {
            producer.produce(new HttpSecurityPolicyBuildItem(e.getKey(),
                    new SupplierImpl<>(new RolesAllowedHttpSecurityPolicy(e.getValue().rolesAllowed))));
        }

    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void initFormAuth(
            BeanContainerBuildItem beanContainerBuildItem,
            HttpSecurityRecorder recorder,
            HttpBuildTimeConfig buildTimeConfig,
            HttpConfiguration httpConfiguration) {
        if (buildTimeConfig.auth.form.enabled) {
            recorder.setupFormAuth(beanContainerBuildItem.getValue(), httpConfiguration, buildTimeConfig);
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupAuthenticationMechanisms(
            HttpSecurityRecorder recorder,
            BuildProducer<FilterBuildItem> filterBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> beanProducer,
            Capabilities capabilities,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListenerBuildItemBuildProducer,
            HttpBuildTimeConfig buildTimeConfig,
            List<HttpSecurityPolicyBuildItem> httpSecurityPolicyBuildItemList) {
        Map<String, Supplier<HttpSecurityPolicy>> policyMap = new HashMap<>();
        for (HttpSecurityPolicyBuildItem e : httpSecurityPolicyBuildItemList) {
            if (policyMap.containsKey(e.getName())) {
                throw new RuntimeException("Multiple HTTP security policies defined with name " + e.getName());
            }
            policyMap.put(e.getName(), e.policySupplier);
        }

        if (buildTimeConfig.auth.form.enabled) {
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(FormAuthenticationMechanism.class));
        } else if (buildTimeConfig.auth.basic) {
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(BasicAuthenticationMechanism.class));
        }

        if (capabilities.isCapabilityPresent(Capabilities.SECURITY)) {
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClass(HttpAuthenticator.class)
                            .addBeanClass(HttpAuthorizer.class).build());
            filterBuildItemBuildProducer
                    .produce(new FilterBuildItem(recorder.authenticationMechanismHandler(), FilterBuildItem.AUTHENTICATION));
            filterBuildItemBuildProducer
                    .produce(new FilterBuildItem(recorder.permissionCheckHandler(), FilterBuildItem.AUTHORIZATION));

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
