package io.quarkus.vertx.http.deployment;

import static io.quarkus.arc.processor.DotNames.APPLICATION_SCOPED;
import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;

import java.security.Permission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.StringPermission;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.PolicyConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.security.AuthenticatedHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.DenySecurityPolicy;
import io.quarkus.vertx.http.runtime.security.EagerSecurityInterceptorStorage;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.HttpAuthorizer;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder;
import io.quarkus.vertx.http.runtime.security.MtlsAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.PathMatchingHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.PermitSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.RolesAllowedHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.SupplierImpl;
import io.quarkus.vertx.http.runtime.security.VertxBlockingSecurityExecutor;
import io.vertx.core.http.ClientAuth;
import io.vertx.ext.web.RoutingContext;

public class HttpSecurityProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void builtins(BuildProducer<HttpSecurityPolicyBuildItem> producer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            CombinedIndexBuildItem combinedIndexBuildItem,
            HttpBuildTimeConfig buildTimeConfig, HttpSecurityRecorder recorder,
            BuildProducer<AdditionalBeanBuildItem> beanProducer) {
        producer.produce(new HttpSecurityPolicyBuildItem("deny", new SupplierImpl<>(new DenySecurityPolicy())));
        producer.produce(new HttpSecurityPolicyBuildItem("permit", new SupplierImpl<>(new PermitSecurityPolicy())));
        producer.produce(
                new HttpSecurityPolicyBuildItem("authenticated", new SupplierImpl<>(new AuthenticatedHttpSecurityPolicy())));
        if (!buildTimeConfig.auth.permissions.isEmpty()) {
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(PathMatchingHttpSecurityPolicy.class));
        }
        Map<String, BiFunction<String, String[], Permission>> permClassToCreator = new HashMap<>();
        for (Map.Entry<String, PolicyConfig> e : buildTimeConfig.auth.rolePolicy.entrySet()) {
            PolicyConfig policyConfig = e.getValue();
            if (policyConfig.permissions.isEmpty()) {
                producer.produce(new HttpSecurityPolicyBuildItem(e.getKey(),
                        new SupplierImpl<>(new RolesAllowedHttpSecurityPolicy(e.getValue().rolesAllowed))));
            } else {
                // create HTTP Security policy that checks allowed roles and grants SecurityIdentity permissions to
                // requests that this policy allows to proceed
                var permissionCreator = permClassToCreator.computeIfAbsent(policyConfig.permissionClass,
                        new Function<String, BiFunction<String, String[], Permission>>() {
                            @Override
                            public BiFunction<String, String[], Permission> apply(String s) {
                                if (StringPermission.class.getName().equals(s)) {
                                    return recorder.stringPermissionCreator();
                                }
                                boolean constructorAcceptsActions = validateConstructor(combinedIndexBuildItem.getIndex(),
                                        policyConfig.permissionClass);
                                return recorder.customPermissionCreator(s, constructorAcceptsActions);
                            }
                        });
                var policy = recorder.createRolesAllowedPolicy(policyConfig.rolesAllowed, policyConfig.permissions,
                        permissionCreator);
                producer.produce(new HttpSecurityPolicyBuildItem(e.getKey(), policy));
            }
        }

        if (!permClassToCreator.isEmpty()) {
            // we need to register Permission classes for reflection as strictly speaking
            // they might not exactly match classes defined via `PermissionsAllowed#permission`
            var permissionClassesArr = permClassToCreator.keySet().toArray(new String[0]);
            reflectiveClassProducer
                    .produce(ReflectiveClassBuildItem.builder(permissionClassesArr).constructors().fields().methods().build());
        }
    }

    private static boolean validateConstructor(IndexView index, String permissionClass) {
        ClassInfo classInfo = index.getClassByName(permissionClass);

        if (classInfo == null) {
            throw new ConfigurationException(String.format("Permission class '%s' is missing", permissionClass));
        }

        // must have exactly one constructor
        if (classInfo.constructors().size() != 1) {
            throw new ConfigurationException(
                    String.format("Permission class '%s' must have exactly one constructor", permissionClass));
        }
        MethodInfo constructor = classInfo.constructors().get(0);

        // first parameter must be permission name (String)
        if (constructor.parametersCount() == 0 || !isString(constructor.parameterType(0))) {
            throw new ConfigurationException(
                    String.format("Permission class '%s' constructor first parameter must be '%s' (permission name)",
                            permissionClass, String.class.getName()));
        }

        // second parameter (actions) is optional
        if (constructor.parametersCount() == 1) {
            // permission constructor accepts just name, no actions
            return false;
        }

        if (constructor.parametersCount() == 2) {
            if (!isStringArray(constructor.parameterType(1))) {
                throw new ConfigurationException(
                        String.format("Permission class '%s' constructor second parameter must be '%s' array", permissionClass,
                                String.class.getName()));
            }
            return true;
        }

        throw new ConfigurationException(String.format(
                "Permission class '%s' constructor must accept either one parameter (String permissionName), or two parameters (String permissionName, String[] actions)",
                permissionClass));
    }

    private static boolean isStringArray(Type type) {
        return type.kind() == Type.Kind.ARRAY && isString(type.asArrayType().constituent());
    }

    private static boolean isString(Type type) {
        return type.kind() == Type.Kind.CLASS && type.name().toString().equals(String.class.getName());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem initFormAuth(
            HttpSecurityRecorder recorder,
            HttpBuildTimeConfig buildTimeConfig,
            BuildProducer<RouteBuildItem> filterBuildItemBuildProducer) {
        if (!buildTimeConfig.auth.proactive) {
            filterBuildItemBuildProducer.produce(RouteBuildItem.builder().route(buildTimeConfig.auth.form.postLocation)
                    .handler(recorder.formAuthPostHandler()).build());
        }
        if (buildTimeConfig.auth.form.enabled) {
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
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            BuildProducer<SecurityInformationBuildItem> securityInformationProducer) {
        if (!applicationBasicAuthRequired(buildTimeConfig, managementInterfaceBuildTimeConfig)) {
            return null;
        }

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(BasicAuthenticationMechanism.class)
                .types(HttpAuthenticationMechanism.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .supplier(recorder.setupBasicAuth(buildTimeConfig));
        if (!buildTimeConfig.auth.form.enabled && !isMtlsClientAuthenticationEnabled(buildTimeConfig)
                && !buildTimeConfig.auth.basic.orElse(false)) {
            //if not explicitly enabled we make this a default bean, so it is the fallback if nothing else is defined
            configurator.defaultBean();
            securityInformationProducer.produce(SecurityInformationBuildItem.BASIC());
        }

        return configurator.done();
    }

    public static boolean applicationBasicAuthRequired(HttpBuildTimeConfig buildTimeConfig,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig) {
        //basic auth explicitly disabled
        if (buildTimeConfig.auth.basic.isPresent() && !buildTimeConfig.auth.basic.get()) {
            return false;
        }
        if (!buildTimeConfig.auth.basic.orElse(false)) {
            if ((buildTimeConfig.auth.form.enabled || isMtlsClientAuthenticationEnabled(buildTimeConfig))
                    || managementInterfaceBuildTimeConfig.auth.basic.orElse(false)) {
                //if form auth is enabled and we are not then we don't install
                return false;
            }
        }

        return true;
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
            List<HttpSecurityPolicyBuildItem> httpSecurityPolicyBuildItemList,
            BuildProducer<SecurityInformationBuildItem> securityInformationProducer) {
        Map<String, Supplier<HttpSecurityPolicy>> policyMap = new HashMap<>();
        for (HttpSecurityPolicyBuildItem e : httpSecurityPolicyBuildItemList) {
            if (policyMap.containsKey(e.getName())) {
                throw new RuntimeException("Multiple HTTP security policies defined with name " + e.getName());
            }
            policyMap.put(e.getName(), e.policySupplier);
        }

        if (!buildTimeConfig.auth.form.enabled && buildTimeConfig.auth.basic.orElse(false)) {
            securityInformationProducer.produce(SecurityInformationBuildItem.BASIC());
        }

        if (capabilities.isPresent(Capability.SECURITY)) {
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable()
                            .addBeanClass(VertxBlockingSecurityExecutor.class).setDefaultScope(APPLICATION_SCOPED).build());
            beanProducer
                    .produce(AdditionalBeanBuildItem.builder().setUnremovable().addBeanClass(HttpAuthenticator.class)
                            .addBeanClass(HttpAuthorizer.class).build());
            filterBuildItemBuildProducer
                    .produce(new FilterBuildItem(
                            recorder.authenticationMechanismHandler(buildTimeConfig.auth.proactive),
                            FilterBuildItem.AUTHENTICATION));
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

    @BuildStep
    void collectEagerSecurityInterceptors(List<EagerSecurityInterceptorCandidateBuildItem> interceptorCandidates,
            HttpBuildTimeConfig buildTimeConfig, Capabilities capabilities,
            BuildProducer<EagerSecurityInterceptorBuildItem> interceptorsProducer) {
        if (!buildTimeConfig.auth.proactive && capabilities.isPresent(Capability.SECURITY)
                && !interceptorCandidates.isEmpty()) {
            List<MethodInfo> allInterceptedMethodInfos = interceptorCandidates
                    .stream()
                    .map(EagerSecurityInterceptorCandidateBuildItem::getMethodInfo)
                    .collect(Collectors.toList());
            Map<RuntimeValue<MethodDescription>, Consumer<RoutingContext>> methodToInterceptor = interceptorCandidates
                    .stream()
                    .collect(Collectors.toMap(EagerSecurityInterceptorCandidateBuildItem::getDescriptionRuntimeValue,
                            EagerSecurityInterceptorCandidateBuildItem::getSecurityInterceptor));
            interceptorsProducer.produce(new EagerSecurityInterceptorBuildItem(allInterceptedMethodInfos, methodToInterceptor));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void produceEagerSecurityInterceptorStorage(HttpSecurityRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> producer,
            Optional<EagerSecurityInterceptorBuildItem> interceptors) {
        if (interceptors.isPresent()) {
            producer.produce(SyntheticBeanBuildItem
                    .configure(EagerSecurityInterceptorStorage.class)
                    .scope(ApplicationScoped.class)
                    .supplier(
                            recorder.createSecurityInterceptorStorage(interceptors.get().methodCandidateToSecurityInterceptor))
                    .unremovable()
                    .done());
        }
    }

    private static boolean isMtlsClientAuthenticationEnabled(HttpBuildTimeConfig buildTimeConfig) {
        return !ClientAuth.NONE.equals(buildTimeConfig.tlsClientAuth);
    }
}
