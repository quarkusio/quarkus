package io.quarkus.vertx.http.deployment;

import static io.quarkus.arc.processor.DotNames.APPLICATION_SCOPED;
import static io.quarkus.arc.processor.DotNames.DEFAULT_BEAN;
import static io.quarkus.arc.processor.DotNames.SINGLETON;
import static io.quarkus.vertx.http.runtime.security.HttpAuthenticator.BASIC_AUTH_ANNOTATION_DETECTED;
import static io.quarkus.vertx.http.runtime.security.HttpAuthenticator.TEST_IF_BASIC_AUTH_IMPLICITLY_REQUIRED;
import static java.util.stream.Collectors.toMap;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.spi.AdditionalSecuredMethodsBuildItem;
import io.quarkus.security.spi.AdditionalSecurityConstrainerEventPropsBuildItem;
import io.quarkus.security.spi.SecurityTransformerUtils;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.EagerSecurityInterceptorStorage;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.HttpAuthorizer;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.AuthenticationHandler;
import io.quarkus.vertx.http.runtime.security.MtlsAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.PathMatchingHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.VertxBlockingSecurityExecutor;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.FormAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.annotation.MTLSAuthentication;
import io.vertx.core.http.ClientAuth;
import io.vertx.ext.web.RoutingContext;

public class HttpSecurityProcessor {

    private static final DotName AUTH_MECHANISM_NAME = DotName.createSimple(HttpAuthenticationMechanism.class);

    private static final DotName BASIC_AUTH_MECH_NAME = DotName.createSimple(BasicAuthenticationMechanism.class);
    private static final DotName BASIC_AUTH_ANNOTATION_NAME = DotName.createSimple(BasicAuthentication.class);

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void produceNamedHttpSecurityPolicies(List<HttpSecurityPolicyBuildItem> httpSecurityPolicyBuildItems,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
            HttpSecurityRecorder recorder) {
        if (!httpSecurityPolicyBuildItems.isEmpty()) {
            httpSecurityPolicyBuildItems.forEach(item -> syntheticBeanProducer
                    .produce(SyntheticBeanBuildItem
                            .configure(HttpSecurityPolicy.class)
                            .named(HttpSecurityPolicy.class.getName() + "." + item.getName())
                            .runtimeValue(recorder.createNamedHttpSecurityPolicy(item.getPolicySupplier(), item.getName()))
                            .addType(HttpSecurityPolicy.class)
                            .scope(Singleton.class)
                            .unremovable()
                            .done()));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    AdditionalBeanBuildItem initFormAuth(
            HttpSecurityRecorder recorder,
            HttpBuildTimeConfig buildTimeConfig,
            BuildProducer<RouteBuildItem> filterBuildItemBuildProducer) {
        if (buildTimeConfig.auth.form.enabled) {
            if (!buildTimeConfig.auth.proactive) {
                filterBuildItemBuildProducer.produce(RouteBuildItem.builder().route(buildTimeConfig.auth.form.postLocation)
                        .handler(recorder.formAuthPostHandler()).build());
            }
            return AdditionalBeanBuildItem.builder().setUnremovable().addBeanClass(FormAuthenticationMechanism.class)
                    .setDefaultScope(SINGLETON).build();
        }
        return null;
    }

    @BuildStep
    AdditionalBeanBuildItem initMtlsClientAuth(HttpBuildTimeConfig buildTimeConfig) {
        if (isMtlsClientAuthenticationEnabled(buildTimeConfig)) {
            return AdditionalBeanBuildItem.builder().setUnremovable().addBeanClass(MtlsAuthenticationMechanism.class)
                    .setDefaultScope(SINGLETON).build();
        }
        return null;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setMtlsCertificateRoleProperties(
            HttpSecurityRecorder recorder,
            HttpConfiguration config,
            HttpBuildTimeConfig buildTimeConfig) {
        if (isMtlsClientAuthenticationEnabled(buildTimeConfig)) {
            recorder.setMtlsCertificateRoleProperties(config);
        }
    }

    @BuildStep(onlyIf = IsApplicationBasicAuthRequired.class)
    void detectBasicAuthImplicitlyRequired(HttpBuildTimeConfig buildTimeConfig,
            BeanRegistrationPhaseBuildItem beanRegistrationPhaseBuildItem, ApplicationIndexBuildItem applicationIndexBuildItem,
            BuildProducer<SystemPropertyBuildItem> systemPropertyProducer,
            List<EagerSecurityInterceptorBindingBuildItem> eagerSecurityInterceptorBindings) {
        if (makeBasicAuthMechDefaultBean(buildTimeConfig)) {
            var appIndex = applicationIndexBuildItem.getIndex();
            boolean noCustomAuthMechanismsDetected = beanRegistrationPhaseBuildItem
                    .getContext()
                    .beans()
                    .filter(b -> b.hasType(AUTH_MECHANISM_NAME))
                    .filter(BeanInfo::isClassBean)
                    .filter(b -> appIndex.getClassByName(b.getBeanClass()) != null)
                    .isEmpty();
            // we can't decide whether custom mechanisms support basic auth or not
            if (noCustomAuthMechanismsDetected) {
                systemPropertyProducer
                        .produce(new SystemPropertyBuildItem(TEST_IF_BASIC_AUTH_IMPLICITLY_REQUIRED, Boolean.TRUE.toString()));
                if (!eagerSecurityInterceptorBindings.isEmpty()) {
                    boolean basicAuthAnnotationUsed = eagerSecurityInterceptorBindings
                            .stream()
                            .map(EagerSecurityInterceptorBindingBuildItem::getAnnotationBindings)
                            .flatMap(Arrays::stream)
                            .anyMatch(BASIC_AUTH_ANNOTATION_NAME::equals);
                    // @BasicAuthentication is used, hence the basic authentication is required
                    if (basicAuthAnnotationUsed) {
                        systemPropertyProducer
                                .produce(new SystemPropertyBuildItem(BASIC_AUTH_ANNOTATION_DETECTED, Boolean.TRUE.toString()));
                    }
                }
            }
        }
    }

    @BuildStep(onlyIf = IsApplicationBasicAuthRequired.class)
    AdditionalBeanBuildItem initBasicAuth(HttpBuildTimeConfig buildTimeConfig,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformerProducer,
            BuildProducer<SecurityInformationBuildItem> securityInformationProducer) {

        if (makeBasicAuthMechDefaultBean(buildTimeConfig)) {
            //if not explicitly enabled we make this a default bean, so it is the fallback if nothing else is defined
            annotationsTransformerProducer.produce(new AnnotationsTransformerBuildItem(AnnotationsTransformer
                    .appliedToClass()
                    .whenClass(cl -> BASIC_AUTH_MECH_NAME.equals(cl.name()))
                    .thenTransform(t -> t.add(DEFAULT_BEAN))));
        }

        if (buildTimeConfig.auth.basic.isPresent() && buildTimeConfig.auth.basic.get()) {
            securityInformationProducer.produce(SecurityInformationBuildItem.BASIC());
        }

        return AdditionalBeanBuildItem.builder().setUnremovable().addBeanClass(BasicAuthenticationMechanism.class).build();
    }

    private static boolean makeBasicAuthMechDefaultBean(HttpBuildTimeConfig buildTimeConfig) {
        return !buildTimeConfig.auth.form.enabled && !isMtlsClientAuthenticationEnabled(buildTimeConfig)
                && !buildTimeConfig.auth.basic.orElse(false);
    }

    private static boolean applicationBasicAuthRequired(HttpBuildTimeConfig buildTimeConfig,
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
            Optional<HttpAuthenticationHandlerBuildItem> authenticationHandlerBuildItem,
            Capabilities capabilities,
            HttpBuildTimeConfig buildTimeConfig,
            BuildProducer<SecurityInformationBuildItem> securityInformationProducer) {
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
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(PathMatchingHttpSecurityPolicy.class));
            filterBuildItemBuildProducer
                    .produce(new FilterBuildItem(
                            recorder.getHttpAuthenticatorHandler(authenticationHandlerBuildItem.get().handler),
                            FilterBuildItem.AUTHENTICATION));
            filterBuildItemBuildProducer
                    .produce(new FilterBuildItem(recorder.permissionCheckHandler(), FilterBuildItem.AUTHORIZATION));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void createHttpAuthenticationHandler(HttpSecurityRecorder recorder, Capabilities capabilities,
            HttpBuildTimeConfig buildTimeConfig,
            BuildProducer<HttpAuthenticationHandlerBuildItem> authenticationHandlerProducer) {
        if (capabilities.isPresent(Capability.SECURITY)) {
            authenticationHandlerProducer.produce(
                    new HttpAuthenticationHandlerBuildItem(
                            recorder.authenticationMechanismHandler(buildTimeConfig.auth.proactive)));
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    @Consume(BeanContainerBuildItem.class)
    void initializeAuthenticationHandler(Optional<HttpAuthenticationHandlerBuildItem> authenticationHandler,
            HttpSecurityRecorder recorder, HttpConfiguration httpConfig) {
        if (authenticationHandler.isPresent()) {
            recorder.initializeHttpAuthenticatorHandler(authenticationHandler.get().handler, httpConfig);
        }
    }

    @BuildStep
    List<HttpAuthMechanismAnnotationBuildItem> registerHttpAuthMechanismAnnotations() {
        return List.of(
                new HttpAuthMechanismAnnotationBuildItem(DotName.createSimple(BasicAuthentication.class),
                        BasicAuthentication.AUTH_MECHANISM_SCHEME),
                new HttpAuthMechanismAnnotationBuildItem(DotName.createSimple(FormAuthentication.class),
                        FormAuthentication.AUTH_MECHANISM_SCHEME),
                new HttpAuthMechanismAnnotationBuildItem(DotName.createSimple(MTLSAuthentication.class),
                        MTLSAuthentication.AUTH_MECHANISM_SCHEME));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerAuthMechanismSelectionInterceptor(Capabilities capabilities, HttpBuildTimeConfig buildTimeConfig,
            BuildProducer<EagerSecurityInterceptorBindingBuildItem> bindingProducer, HttpSecurityRecorder recorder,
            BuildProducer<AdditionalSecuredMethodsBuildItem> additionalSecuredMethodsProducer,
            List<HttpAuthMechanismAnnotationBuildItem> additionalHttpAuthMechAnnotations,
            CombinedIndexBuildItem combinedIndexBuildItem) {
        // methods annotated with @HttpAuthenticationMechanism that we should additionally secure;
        // when there is no other RBAC annotation applied
        // then by default @HttpAuthenticationMechanism("any-value") == @Authenticated
        Set<MethodInfo> methodsWithoutRbacAnnotations = new HashSet<>();

        DotName[] mechNames = Stream
                .concat(Stream.of(AUTH_MECHANISM_NAME), additionalHttpAuthMechAnnotations.stream().map(s -> s.annotationName))
                .flatMap(mechName -> {
                    var instances = combinedIndexBuildItem.getIndex().getAnnotations(mechName);
                    if (!instances.isEmpty()) {
                        // e.g. collect @Basic without @RolesAllowed, @PermissionsAllowed, ..
                        methodsWithoutRbacAnnotations
                                .addAll(collectMethodsWithoutRbacAnnotation(collectAnnotatedMethods(instances)));
                        methodsWithoutRbacAnnotations
                                .addAll(collectClassMethodsWithoutRbacAnnotation(collectAnnotatedClasses(instances)));
                        return Stream.of(mechName);
                    } else {
                        return Stream.empty();
                    }
                }).toArray(DotName[]::new);

        if (mechNames.length > 0) {
            validateAuthMechanismAnnotationUsage(capabilities, buildTimeConfig, mechNames);

            // register method interceptor that will be run before security checks
            Map<String, String> knownBindingValues = additionalHttpAuthMechAnnotations.stream()
                    .collect(Collectors.toMap(item -> item.annotationName.toString(), item -> item.authMechanismScheme));
            bindingProducer.produce(new EagerSecurityInterceptorBindingBuildItem(
                    recorder.authMechanismSelectionInterceptorCreator(), knownBindingValues, mechNames));
            recorder.selectAuthMechanismViaAnnotation();

            // make all @HttpAuthenticationMechanism annotation targets authenticated by default
            if (!methodsWithoutRbacAnnotations.isEmpty()) {
                // @RolesAllowed("**") == @Authenticated
                additionalSecuredMethodsProducer.produce(
                        new AdditionalSecuredMethodsBuildItem(methodsWithoutRbacAnnotations, Optional.of(List.of("**"))));
            }
        }
    }

    @BuildStep
    void collectInterceptedMethods(CombinedIndexBuildItem indexBuildItem,
            List<EagerSecurityInterceptorBindingBuildItem> interceptorBindings,
            BuildProducer<EagerSecurityInterceptorMethodsBuildItem> methodsProducer) {
        if (!interceptorBindings.isEmpty()) {
            var index = indexBuildItem.getIndex();
            Map<MethodInfo, List<EagerSecurityInterceptorBindingBuildItem>> cache = new HashMap<>();
            Map<DotName, Map<String, List<MethodInfo>>> result = new HashMap<>();
            addInterceptedEndpoints(interceptorBindings, index, AnnotationTarget.Kind.METHOD, result, cache);
            addInterceptedEndpoints(interceptorBindings, index, AnnotationTarget.Kind.CLASS, result, cache);
            if (!result.isEmpty()) {
                result.forEach((annotationBinding, bindingValueToInterceptedMethods) -> methodsProducer.produce(
                        new EagerSecurityInterceptorMethodsBuildItem(bindingValueToInterceptedMethods, annotationBinding)));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void produceEagerSecurityInterceptorStorage(HttpSecurityRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> producer,
            List<EagerSecurityInterceptorBindingBuildItem> interceptorBindings,
            List<EagerSecurityInterceptorMethodsBuildItem> interceptorMethods) {
        if (!interceptorMethods.isEmpty()) {
            final var bindingNameToInterceptorCreator = interceptorBindings
                    .stream()
                    .flatMap(binding -> Arrays.stream(binding.getAnnotationBindings())
                            .map(name -> Map.entry(name, binding.getInterceptorCreator())))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

            final var methodCache = new HashMap<MethodInfo, RuntimeValue<MethodDescription>>();
            final var methodDescriptionToInterceptor = new HashMap<RuntimeValue<MethodDescription>, Consumer<RoutingContext>>();
            for (EagerSecurityInterceptorMethodsBuildItem interceptorMethod : interceptorMethods) {
                var interceptorCreator = bindingNameToInterceptorCreator.get(interceptorMethod.interceptorBinding);
                for (Map.Entry<String, List<MethodInfo>> e : interceptorMethod.bindingValueToInterceptedMethods.entrySet()) {
                    var annotationValue = e.getKey();
                    var annotatedMethods = e.getValue();
                    var interceptor = recorder.createEagerSecurityInterceptor(interceptorCreator, annotationValue);
                    for (MethodInfo method : annotatedMethods) {
                        // transform method info to description
                        final RuntimeValue<MethodDescription> methodDescription = methodCache
                                .computeIfAbsent(method, mi -> {
                                    String[] paramTypes = mi.parameterTypes().stream().map(t -> t.name().toString())
                                            .toArray(String[]::new);
                                    String className = mi.declaringClass().name().toString();
                                    String methodName = mi.name();
                                    return recorder.createMethodDescription(className, methodName, paramTypes);
                                });

                        // add (methodDesc -> interceptor) to the storage
                        methodDescriptionToInterceptor.compute(methodDescription,
                                (desc, existingInterceptor) -> existingInterceptor == null ? interceptor
                                        : recorder.compoundSecurityInterceptor(interceptor, existingInterceptor));
                    }
                }
            }

            producer.produce(SyntheticBeanBuildItem
                    .configure(EagerSecurityInterceptorStorage.class)
                    .scope(ApplicationScoped.class)
                    .supplier(recorder.createSecurityInterceptorStorage(methodDescriptionToInterceptor))
                    .unremovable()
                    .done());
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void addRoutingCtxToSecurityEventsForCdiBeans(HttpSecurityRecorder recorder, Capabilities capabilities,
            BuildProducer<AdditionalSecurityConstrainerEventPropsBuildItem> producer) {
        if (capabilities.isPresent(Capability.SECURITY)) {
            producer.produce(
                    new AdditionalSecurityConstrainerEventPropsBuildItem(recorder.createAdditionalSecEventPropsSupplier()));
        }
    }

    private static void validateAuthMechanismAnnotationUsage(Capabilities capabilities, HttpBuildTimeConfig buildTimeConfig,
            DotName[] annotationNames) {
        if (buildTimeConfig.auth.proactive
                || (!capabilities.isPresent(Capability.RESTEASY_REACTIVE) && !capabilities.isPresent(Capability.RESTEASY))) {
            throw new ConfigurationException("Annotations '" + Arrays.toString(annotationNames) + "' can only be used when"
                    + " proactive authentication is disabled and either RESTEasy Reactive or RESTEasy Classic"
                    + " extension is present");
        }
    }

    private static boolean isMtlsClientAuthenticationEnabled(HttpBuildTimeConfig buildTimeConfig) {
        return !ClientAuth.NONE.equals(buildTimeConfig.tlsClientAuth);
    }

    private static Set<MethodInfo> collectClassMethodsWithoutRbacAnnotation(Collection<ClassInfo> classes) {
        return classes
                .stream()
                .filter(c -> !SecurityTransformerUtils.hasSecurityAnnotation(c))
                .map(ClassInfo::methods)
                .flatMap(Collection::stream)
                .filter(HttpSecurityProcessor::hasProperEndpointModifiers)
                .filter(m -> !SecurityTransformerUtils.hasSecurityAnnotation(m))
                .collect(Collectors.toSet());
    }

    private static Set<MethodInfo> collectMethodsWithoutRbacAnnotation(Collection<MethodInfo> methods) {
        return methods
                .stream()
                .filter(m -> !SecurityTransformerUtils.hasSecurityAnnotation(m))
                .collect(Collectors.toSet());
    }

    private static Set<ClassInfo> collectAnnotatedClasses(Collection<AnnotationInstance> instances) {
        return instances
                .stream()
                .map(AnnotationInstance::target)
                .filter(target -> target.kind() == AnnotationTarget.Kind.CLASS)
                .map(AnnotationTarget::asClass)
                .collect(Collectors.toSet());
    }

    private static Set<MethodInfo> collectAnnotatedMethods(Collection<AnnotationInstance> instances) {
        return instances
                .stream()
                .map(AnnotationInstance::target)
                .filter(target -> target.kind() == AnnotationTarget.Kind.METHOD)
                .map(AnnotationTarget::asMethod)
                .collect(Collectors.toSet());
    }

    private static boolean hasProperEndpointModifiers(MethodInfo info) {
        // synthetic methods are not endpoints
        if (info.isSynthetic()) {
            return false;
        }
        if (!Modifier.isPublic(info.flags())) {
            return false;
        }
        if (info.isConstructor()) {
            return false;
        }
        // instance methods only
        return !Modifier.isStatic(info.flags());
    }

    private static void addInterceptedEndpoints(List<EagerSecurityInterceptorBindingBuildItem> interceptorBindings,
            IndexView index, AnnotationTarget.Kind appliesTo, Map<DotName, Map<String, List<MethodInfo>>> result,
            Map<MethodInfo, List<EagerSecurityInterceptorBindingBuildItem>> cache) {
        for (EagerSecurityInterceptorBindingBuildItem interceptorBinding : interceptorBindings) {
            for (DotName annotationBinding : interceptorBinding.getAnnotationBindings()) {
                Map<String, List<MethodInfo>> bindingValueToInterceptedMethods = new HashMap<>();
                for (AnnotationInstance annotation : index.getAnnotations(annotationBinding)) {
                    if (annotation.target().kind() != appliesTo) {
                        continue;
                    }
                    if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                        for (MethodInfo method : annotation.target().asClass().methods()) {
                            if (hasProperEndpointModifiers(method)) {
                                // avoid situation when resource method is annotated with @Basic, class is annotated
                                // with @Bearer, and we apply the @Bearer annotation
                                boolean interceptorBindingNotAppliedOnMethodLevel = !cache.containsKey(method)
                                        || !cache.get(method).contains(interceptorBinding);

                                if (interceptorBindingNotAppliedOnMethodLevel) {
                                    addInterceptedEndpoint(method, annotation, annotationBinding,
                                            bindingValueToInterceptedMethods, interceptorBinding);
                                }
                            }
                        }
                    } else {
                        MethodInfo mi = annotation.target().asMethod();

                        // endpoint can only be annotated with one of @Basic, @Form, ...
                        // however combining @CodeFlow and @Tenant is supported
                        var appliedBindings = cache.computeIfAbsent(mi, a -> new ArrayList<>());
                        if (appliedBindings.contains(interceptorBinding)) {
                            throw new RuntimeException(
                                    "Only one of the '%s' annotations can be applied on the '%s' method".formatted(
                                            interceptorBinding.getAnnotationBindings(), mi.declaringClass().name() + "#" + mi));
                        } else {
                            appliedBindings.add(interceptorBinding);
                        }

                        addInterceptedEndpoint(mi, annotation, annotationBinding, bindingValueToInterceptedMethods,
                                interceptorBinding);
                    }
                }
                if (!bindingValueToInterceptedMethods.isEmpty()) {
                    result.compute(annotationBinding, (key, existingMap) -> {
                        if (existingMap == null) {
                            return bindingValueToInterceptedMethods;
                        } else {
                            bindingValueToInterceptedMethods.forEach((annotationValue, methods) -> existingMap
                                    .computeIfAbsent(annotationValue, a -> new ArrayList<>()).addAll(methods));
                            return existingMap;
                        }
                    });
                }
            }
        }
    }

    private static void addInterceptedEndpoint(MethodInfo classEndpoint, AnnotationInstance annotationInstance,
            DotName annotation, Map<String, List<MethodInfo>> bindingValueToInterceptedMethods,
            EagerSecurityInterceptorBindingBuildItem interceptorBinding) {
        bindingValueToInterceptedMethods
                .computeIfAbsent(interceptorBinding.getBindingValue(annotationInstance, annotation, classEndpoint),
                        s -> new ArrayList<>())
                .add(classEndpoint);
    }

    static class IsApplicationBasicAuthRequired implements BooleanSupplier {
        private final boolean required;

        public IsApplicationBasicAuthRequired(HttpBuildTimeConfig httpBuildTimeConfig,
                ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig) {
            required = applicationBasicAuthRequired(httpBuildTimeConfig, managementInterfaceBuildTimeConfig);
        }

        @Override
        public boolean getAsBoolean() {
            return required;
        }
    }

    static final class HttpAuthenticationHandlerBuildItem extends SimpleBuildItem {
        private final RuntimeValue<AuthenticationHandler> handler;

        private HttpAuthenticationHandlerBuildItem(RuntimeValue<AuthenticationHandler> handler) {
            this.handler = handler;
        }
    }
}
