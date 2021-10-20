package io.quarkus.rest.client.reactive.deployment;

import static io.quarkus.arc.processor.MethodDescriptors.MAP_PUT;
import static io.quarkus.rest.client.reactive.deployment.DotNames.REGISTER_CLIENT_HEADERS;
import static io.quarkus.rest.client.reactive.deployment.DotNames.REGISTER_PROVIDER;
import static io.quarkus.rest.client.reactive.deployment.DotNames.REGISTER_PROVIDERS;
import static java.util.Arrays.asList;
import static org.jboss.resteasy.reactive.common.processor.EndpointIndexer.CDI_WRAPPER_SUFFIX;
import static org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveScanner.BUILTIN_HTTP_ANNOTATIONS_TO_METHOD;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.ScopeInfo;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigurationTypeBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.jaxrs.client.reactive.deployment.JaxrsClientReactiveEnricherBuildItem;
import io.quarkus.jaxrs.client.reactive.deployment.RestClientDefaultConsumesBuildItem;
import io.quarkus.jaxrs.client.reactive.deployment.RestClientDefaultProducesBuildItem;
import io.quarkus.jaxrs.client.reactive.deployment.RestClientDisableSmartDefaultProduces;
import io.quarkus.rest.client.reactive.runtime.AnnotationRegisteredProviders;
import io.quarkus.rest.client.reactive.runtime.HeaderCapturingServerFilter;
import io.quarkus.rest.client.reactive.runtime.HeaderContainer;
import io.quarkus.rest.client.reactive.runtime.RestClientReactiveCDIWrapperBase;
import io.quarkus.rest.client.reactive.runtime.RestClientReactiveConfig;
import io.quarkus.rest.client.reactive.runtime.RestClientRecorder;
import io.quarkus.restclient.config.RestClientConfigUtils;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.resteasy.reactive.spi.ContainerRequestFilterBuildItem;

class RestClientReactiveProcessor {

    private static final Logger log = Logger.getLogger(RestClientReactiveProcessor.class);

    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class.getName());
    private static final DotName SESSION_SCOPED = DotName.createSimple(SessionScoped.class.getName());

    private static final String DISABLE_SMART_PRODUCES_QUARKUS = "quarkus.rest-client.disable-smart-produces";

    @BuildStep
    void announceFeature(BuildProducer<FeatureBuildItem> features) {
        features.produce(new FeatureBuildItem(Feature.REST_CLIENT_REACTIVE));
    }

    @BuildStep
    void registerQueryParamStyleForConfig(BuildProducer<ConfigurationTypeBuildItem> configurationTypes) {
        configurationTypes.produce(new ConfigurationTypeBuildItem(QueryParamStyle.class));
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(Feature.REST_CLIENT_REACTIVE);
    }

    @BuildStep
    void setUpDefaultMediaType(BuildProducer<RestClientDefaultConsumesBuildItem> consumes,
            BuildProducer<RestClientDefaultProducesBuildItem> produces,
            BuildProducer<RestClientDisableSmartDefaultProduces> disableSmartProduces,
            RestClientReactiveConfig config) {
        consumes.produce(new RestClientDefaultConsumesBuildItem(MediaType.APPLICATION_JSON, 10));
        produces.produce(new RestClientDefaultProducesBuildItem(MediaType.APPLICATION_JSON, 10));
        Config mpConfig = ConfigProvider.getConfig();
        Optional<Boolean> disableSmartProducesConfig = mpConfig.getOptionalValue(DISABLE_SMART_PRODUCES_QUARKUS, Boolean.class);
        if (config.disableSmartProduces || disableSmartProducesConfig.orElse(false)) {
            disableSmartProduces.produce(new RestClientDisableSmartDefaultProduces());
        }
    }

    @BuildStep
    void registerRestClientListenerForTracing(
            Capabilities capabilities,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        if (capabilities.isPresent(Capability.SMALLRYE_OPENTRACING)) {
            resource.produce(new NativeImageResourceBuildItem(
                    "META-INF/services/org.eclipse.microprofile.rest.client.spi.RestClientListener"));
            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, false, false,
                            "io.smallrye.opentracing.SmallRyeRestClientListener"));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            RestClientRecorder restClientRecorder) {
        restClientRecorder.setRestClientBuilderResolver();
        additionalBeans.produce(new AdditionalBeanBuildItem(RestClient.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(HeaderContainer.class));
    }

    @BuildStep
    UnremovableBeanBuildItem makeConfigUnremovable() {
        return UnremovableBeanBuildItem.beanTypes(RestClientsConfig.class);
    }

    @BuildStep
    void setupRequestCollectingFilter(BuildProducer<ContainerRequestFilterBuildItem> filters) {
        filters.produce(new ContainerRequestFilterBuildItem(HeaderCapturingServerFilter.class.getName()));
    }

    @BuildStep
    void addMpClientEnricher(BuildProducer<JaxrsClientReactiveEnricherBuildItem> enrichers) {
        enrichers.produce(new JaxrsClientReactiveEnricherBuildItem(new MicroProfileRestClientEnricher()));
    }

    private void searchForJaxRsMethods(List<MethodInfo> listOfKnownMethods, ClassInfo startingInterface, CompositeIndex index) {
        for (MethodInfo method : startingInterface.methods()) {
            if (isRestMethod(method)) {
                listOfKnownMethods.add(method);
            }
        }
        List<DotName> otherImplementedInterfaces = startingInterface.interfaceNames();
        for (DotName otherInterface : otherImplementedInterfaces) {
            ClassInfo superInterface = index.getClassByName(otherInterface);
            if (superInterface != null)
                searchForJaxRsMethods(listOfKnownMethods, superInterface, index);
        }
    }

    @BuildStep
    void registerHeaderFactoryBeans(CombinedIndexBuildItem index,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        Collection<AnnotationInstance> annotations = index.getIndex().getAnnotations(REGISTER_CLIENT_HEADERS);

        for (AnnotationInstance registerClientHeaders : annotations) {
            AnnotationValue value = registerClientHeaders.value();
            if (value != null) {
                Type clientHeaderFactoryType = value.asClass();
                String factoryTypeName = clientHeaderFactoryType.name().toString();
                if (!MicroProfileRestClientEnricher.DEFAULT_HEADERS_FACTORY.equals(factoryTypeName)) {
                    additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(factoryTypeName));
                }
            }
        }
    }

    /**
     * Creates an implementation of `AnnotationRegisteredProviders` class with a constructor that:
     * <ul>
     * <li>puts all the providers registered by the @RegisterProvider annotation in a
     * map using the {@link AnnotationRegisteredProviders#addProviders(String, Map)} method</li>
     * <li>registers all the provider implementations annotated with @Provider using
     * {@link AnnotationRegisteredProviders#addGlobalProvider(Class, int)}</li>
     * </ul>
     * 
     *
     * @param indexBuildItem index
     * @param generatedBeans build producer for generated beans
     */
    @BuildStep
    void registerProvidersFromAnnotations(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            RestClientReactiveConfig clientConfig) {
        String annotationRegisteredProvidersImpl = AnnotationRegisteredProviders.class.getName() + "Implementation";
        IndexView index = indexBuildItem.getIndex();
        Map<String, List<AnnotationInstance>> annotationsByClassName = new HashMap<>();

        for (AnnotationInstance annotation : index.getAnnotations(REGISTER_PROVIDER)) {
            String targetClass = annotation.target().asClass().name().toString();
            annotationsByClassName.computeIfAbsent(targetClass, key -> new ArrayList<>())
                    .add(annotation);
        }

        for (AnnotationInstance annotation : index.getAnnotations(REGISTER_PROVIDERS)) {
            String targetClass = annotation.target().asClass().name().toString();
            annotationsByClassName.computeIfAbsent(targetClass, key -> new ArrayList<>())
                    .addAll(asList(annotation.value().asNestedArray()));
        }

        try (ClassCreator classCreator = ClassCreator.builder()
                .className(annotationRegisteredProvidersImpl)
                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeans))
                .superClass(AnnotationRegisteredProviders.class)
                .build()) {

            classCreator.addAnnotation(Singleton.class.getName());
            MethodCreator constructor = classCreator
                    .getMethodCreator(MethodDescriptor.ofConstructor(annotationRegisteredProvidersImpl));
            constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(AnnotationRegisteredProviders.class),
                    constructor.getThis());

            if (clientConfig.providerAutodiscovery) {
                for (AnnotationInstance instance : index.getAnnotations(ResteasyReactiveDotNames.PROVIDER)) {
                    ClassInfo providerClass = instance.target().asClass();

                    // ignore providers annotated with `@ConstrainedTo(SERVER)`
                    AnnotationInstance constrainedToInstance = providerClass
                            .classAnnotation(ResteasyReactiveDotNames.CONSTRAINED_TO);
                    if (constrainedToInstance != null) {
                        if (RuntimeType.valueOf(constrainedToInstance.value().asEnum()) == RuntimeType.SERVER) {
                            continue;
                        }
                    }

                    if (providerClass.interfaceNames().contains(ResteasyReactiveDotNames.FEATURE)) {
                        continue; // features should not be automatically registered for the client, see javadoc for Feature
                    }

                    int priority = getAnnotatedPriority(index, providerClass.name().toString(), Priorities.USER);

                    constructor.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AnnotationRegisteredProviders.class, "addGlobalProvider",
                                    void.class, Class.class,
                                    int.class),
                            constructor.getThis(), constructor.loadClass(providerClass.name().toString()),
                            constructor.load(priority));
                }
            }

            for (Map.Entry<String, List<AnnotationInstance>> annotationsForClass : annotationsByClassName.entrySet()) {
                ResultHandle map = constructor.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
                for (AnnotationInstance value : annotationsForClass.getValue()) {
                    String className = value.value().asString();
                    AnnotationValue priorityAnnotationValue = value.value("priority");
                    int priority;
                    if (priorityAnnotationValue == null) {
                        priority = getAnnotatedPriority(index, className, Priorities.USER);
                    } else {
                        priority = priorityAnnotationValue.asInt();
                    }

                    constructor.invokeInterfaceMethod(MAP_PUT, map, constructor.loadClass(className),
                            constructor.load(priority));
                }
                constructor.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(AnnotationRegisteredProviders.class, "addProviders", void.class, String.class,
                                Map.class),
                        constructor.getThis(), constructor.load(annotationsForClass.getKey()), map);
            }

            constructor.returnValue(null);
        }

        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(annotationRegisteredProvidersImpl));
    }

    private int getAnnotatedPriority(IndexView index, String className, int defaultPriority) {
        ClassInfo providerClass = index.getClassByName(DotName.createSimple(className));
        int priority = defaultPriority;
        if (providerClass == null) {
            log.warnv("Unindexed provider class {0}. The priority of the provider will be set to {1}. ", className,
                    defaultPriority);
        } else {
            AnnotationInstance priorityAnnoOnProvider = providerClass.classAnnotation(ResteasyReactiveDotNames.PRIORITY);
            if (priorityAnnoOnProvider != null) {
                priority = priorityAnnoOnProvider.value().asInt();
            }
        }
        return priority;
    }

    @BuildStep
    AdditionalBeanBuildItem registerProviderBeans(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();
        List<AnnotationInstance> allInstances = new ArrayList<>(index.getAnnotations(REGISTER_PROVIDER));
        for (AnnotationInstance annotation : index.getAnnotations(REGISTER_PROVIDERS)) {
            allInstances.addAll(asList(annotation.value().asNestedArray()));
        }
        allInstances.addAll(index.getAnnotations(REGISTER_CLIENT_HEADERS));
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();
        for (AnnotationInstance annotationInstance : allInstances) {
            // Make sure all providers not annotated with @Provider but used in @RegisterProvider are registered as beans
            AnnotationValue value = annotationInstance.value();
            if (value != null) {
                builder.addBeanClass(value.asClass().toString());
            }
        }
        return builder.build();
    }

    @BuildStep
    void addRestClientBeans(Capabilities capabilities,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            RestClientReactiveConfig clientConfig) {

        CompositeIndex index = CompositeIndex.create(combinedIndexBuildItem.getIndex());
        Set<AnnotationInstance> registerRestClientAnnos = new HashSet<>(index.getAnnotations(REGISTER_REST_CLIENT));
        for (AnnotationInstance registerRestClient : registerRestClientAnnos) {
            ClassInfo jaxrsInterface = registerRestClient.target().asClass();
            // for each interface annotated with @RegisterRestClient, generate a $$CDIWrapper CDI bean that can be injected
            if (Modifier.isAbstract(jaxrsInterface.flags())) {
                List<MethodInfo> restMethods = new ArrayList<>();

                // search this class and its super interfaces for jaxrs methods
                searchForJaxRsMethods(restMethods, jaxrsInterface, index);
                if (restMethods.isEmpty()) {
                    continue;
                }

                String wrapperClassName = jaxrsInterface.name().toString() + CDI_WRAPPER_SUFFIX;
                try (ClassCreator classCreator = ClassCreator.builder()
                        .className(wrapperClassName)
                        .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeans))
                        .interfaces(jaxrsInterface.name().toString())
                        .superClass(RestClientReactiveCDIWrapperBase.class)
                        .build()) {

                    // CLASS LEVEL
                    final Optional<String> configKey = getConfigKey(registerRestClient);
                    final ScopeInfo scope = computeDefaultScope(capabilities, ConfigProvider.getConfig(), jaxrsInterface,
                            configKey, clientConfig);
                    // add a scope annotation, e.g. @Singleton
                    classCreator.addAnnotation(scope.getDotName().toString());
                    classCreator.addAnnotation(RestClient.class);
                    // e.g. @Typed({InterfaceClass.class})
                    // needed for CDI to inject the proper wrapper in case of
                    // subinterfaces
                    org.objectweb.asm.Type asmType = org.objectweb.asm.Type
                            .getObjectType(jaxrsInterface.name().toString().replace('.', '/'));
                    classCreator.addAnnotation(Typed.class.getName(), RetentionPolicy.RUNTIME)
                            .addValue("value", new org.objectweb.asm.Type[] { asmType });

                    // CONSTRUCTOR:

                    MethodCreator constructor = classCreator
                            .getMethodCreator(MethodDescriptor.ofConstructor(classCreator.getClassName()));

                    AnnotationValue baseUri = registerRestClient.value("baseUri");

                    ResultHandle baseUriHandle = constructor.load(baseUri != null ? baseUri.asString() : "");
                    constructor.invokeSpecialMethod(
                            MethodDescriptor.ofConstructor(RestClientReactiveCDIWrapperBase.class, Class.class, String.class,
                                    String.class),
                            constructor.getThis(),
                            constructor.loadClass(jaxrsInterface.toString()),
                            baseUriHandle,
                            configKey.isPresent() ? constructor.load(configKey.get()) : constructor.loadNull());
                    constructor.returnValue(null);

                    // METHODS:
                    for (MethodInfo method : restMethods) {
                        // for each method that corresponds to making a rest call, create a method like:
                        // public JsonArray get() {
                        //      return ((InterfaceClass)this.getDelegate()).get();
                        // }
                        MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.of(method));
                        methodCreator.setSignature(AsmUtil.getSignatureIfRequired(method));

                        // copy method annotations, there can be interceptors bound to them:
                        for (AnnotationInstance annotation : method.annotations()) {
                            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD
                                    && !BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.containsKey(annotation.name())
                                    && !ResteasyReactiveDotNames.PATH.equals(annotation.name())) {
                                methodCreator.addAnnotation(annotation);
                            }
                        }

                        ResultHandle delegate = methodCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(RestClientReactiveCDIWrapperBase.class, "getDelegate",
                                        Object.class),
                                methodCreator.getThis());

                        int parameterCount = method.parameters().size();
                        ResultHandle result;
                        if (parameterCount == 0) {
                            result = methodCreator.invokeInterfaceMethod(method, delegate);
                        } else {
                            ResultHandle[] params = new ResultHandle[parameterCount];
                            for (int i = 0; i < parameterCount; i++) {
                                params[i] = methodCreator.getMethodParam(i);
                            }
                            result = methodCreator.invokeInterfaceMethod(method, delegate, params);
                        }
                        methodCreator.returnValue(result);
                    }
                }
            }
        }
    }

    private boolean isRestMethod(MethodInfo method) {
        if (!Modifier.isAbstract(method.flags())) {
            return false;
        }
        for (AnnotationInstance annotation : method.annotations()) {
            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD
                    && BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.containsKey(annotation.name())) {
                return true;
            } else if (annotation.name().equals(ResteasyReactiveDotNames.PATH)) {
                return true;
            }
        }
        return false;
    }

    private Optional<String> getConfigKey(AnnotationInstance registerRestClientAnnotation) {
        AnnotationValue configKeyValue = registerRestClientAnnotation.value("configKey");
        return configKeyValue != null
                ? Optional.of(configKeyValue.asString())
                : Optional.empty();
    }

    private ScopeInfo computeDefaultScope(Capabilities capabilities, Config config,
            ClassInfo restClientInterface,
            Optional<String> configKey,
            RestClientReactiveConfig mpClientConfig) {
        ScopeInfo scopeToUse = null;

        Optional<String> scopeConfig = RestClientConfigUtils.findConfiguredScope(config, restClientInterface, configKey);

        BuiltinScope globalDefaultScope = BuiltinScope.from(DotName.createSimple(mpClientConfig.scope));
        if (globalDefaultScope == null) {
            log.warnv("Unable to map the global rest client scope: '{}' to a scope. Using @ApplicationScoped",
                    mpClientConfig.scope);
            globalDefaultScope = BuiltinScope.APPLICATION;
        }

        if (scopeConfig.isPresent()) {
            final DotName scope = DotName.createSimple(scopeConfig.get());
            final BuiltinScope builtinScope = builtinScopeFromName(scope);
            if (builtinScope != null) { // override default @Dependent scope with user defined one.
                scopeToUse = builtinScope.getInfo();
            } else if (capabilities.isPresent(Capability.SERVLET)) {
                if (scope.equals(SESSION_SCOPED) || scope.toString().equalsIgnoreCase(SESSION_SCOPED.withoutPackagePrefix())) {
                    scopeToUse = new ScopeInfo(SESSION_SCOPED, true);
                }
            }

            if (scopeToUse == null) {
                log.warnf("Unsupported default scope {} provided for rest client {}. Defaulting to {}",
                        scope, restClientInterface.name(), globalDefaultScope.getName());
                scopeToUse = BuiltinScope.DEPENDENT.getInfo();
            }
        } else {
            final Set<DotName> annotations = restClientInterface.annotations().keySet();
            for (final DotName annotationName : annotations) {
                final BuiltinScope builtinScope = BuiltinScope.from(annotationName);
                if (builtinScope != null) {
                    scopeToUse = builtinScope.getInfo();
                    break;
                }
                if (annotationName.equals(SESSION_SCOPED)) {
                    scopeToUse = new ScopeInfo(SESSION_SCOPED, true);
                    break;
                }
            }
        }

        // Initialize a default @Dependent scope as per the spec
        return scopeToUse != null ? scopeToUse : globalDefaultScope.getInfo();
    }

    private BuiltinScope builtinScopeFromName(DotName scopeName) {
        BuiltinScope scope = BuiltinScope.from(scopeName);
        if (scope == null) {
            for (BuiltinScope builtinScope : BuiltinScope.values()) {
                if (builtinScope.getName().withoutPackagePrefix().equalsIgnoreCase(scopeName.toString())) {
                    scope = builtinScope;
                }
            }
        }
        return scope;
    }
}
