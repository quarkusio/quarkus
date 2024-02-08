package io.quarkus.rest.client.reactive.deployment;

import static io.quarkus.arc.processor.MethodDescriptors.MAP_PUT;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_EXCEPTION_MAPPER;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_FORM_PARAM;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_FORM_PARAMS;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_HEADER_PARAM;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_HEADER_PARAMS;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_QUERY_PARAM;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_QUERY_PARAMS;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_REDIRECT_HANDLER;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_REQUEST_FILTER;
import static io.quarkus.rest.client.reactive.deployment.DotNames.CLIENT_RESPONSE_FILTER;
import static io.quarkus.rest.client.reactive.deployment.DotNames.REGISTER_CLIENT_HEADERS;
import static io.quarkus.rest.client.reactive.deployment.DotNames.REGISTER_PROVIDER;
import static io.quarkus.rest.client.reactive.deployment.DotNames.REGISTER_PROVIDERS;
import static io.quarkus.rest.client.reactive.deployment.DotNames.RESPONSE_EXCEPTION_MAPPER;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.jboss.resteasy.reactive.common.processor.EndpointIndexer.CDI_WRAPPER_SUFFIX;
import static org.jboss.resteasy.reactive.common.processor.JandexUtil.isImplementorOf;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.APPLICATION;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.BLOCKING;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REQUEST_SCOPED;
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
import java.util.stream.Collectors;

import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
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
import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.interceptors.ClientGZIPDecodingInterceptor;
import org.jboss.resteasy.reactive.client.spi.MissingMessageBodyReaderErrorMessageContextualizer;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationStore;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.ScopeInfo;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigurationTypeBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
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
import io.quarkus.rest.client.reactive.spi.RestClientAnnotationsTransformerBuildItem;
import io.quarkus.restclient.config.RestClientsBuildTimeConfig;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.restclient.config.deployment.RestClientConfigUtils;
import io.quarkus.resteasy.reactive.spi.ContainerRequestFilterBuildItem;
import io.quarkus.runtime.LaunchMode;

class RestClientReactiveProcessor {

    private static final Logger log = Logger.getLogger(RestClientReactiveProcessor.class);

    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class.getName());
    private static final DotName SESSION_SCOPED = DotName.createSimple(SessionScoped.class.getName());
    private static final DotName KOTLIN_METADATA_ANNOTATION = DotName.createSimple("kotlin.Metadata");

    private static final String DISABLE_SMART_PRODUCES_QUARKUS = "quarkus.rest-client.disable-smart-produces";
    private static final String ENABLE_COMPRESSION = "quarkus.http.enable-compression";
    private static final String KOTLIN_INTERFACE_DEFAULT_IMPL_SUFFIX = "$DefaultImpls";

    private static final Set<DotName> SKIP_COPYING_ANNOTATIONS_TO_GENERATED_CLASS = Set.of(
            REGISTER_REST_CLIENT,
            REGISTER_PROVIDER,
            REGISTER_PROVIDERS,
            CLIENT_HEADER_PARAM,
            CLIENT_HEADER_PARAMS,
            CLIENT_QUERY_PARAM,
            CLIENT_QUERY_PARAMS,
            CLIENT_FORM_PARAM,
            CLIENT_FORM_PARAMS,
            REGISTER_CLIENT_HEADERS);

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
    ServiceProviderBuildItem nativeSpiSupport() {
        return ServiceProviderBuildItem
                .allProvidersFromClassPath(MissingMessageBodyReaderErrorMessageContextualizer.class.getName());
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
    @Record(ExecutionTime.STATIC_INIT)
    void setupAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            RestClientRecorder restClientRecorder) {
        restClientRecorder.setRestClientBuilderResolver();
        additionalBeans.produce(new AdditionalBeanBuildItem(RestClient.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(HeaderContainer.class));
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(RestClientsConfig.class, ClientLogger.class);
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

    @BuildStep
    public void registerProvidersInstances(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<RegisterProviderAnnotationInstanceBuildItem> producer) {
        IndexView index = indexBuildItem.getIndex();

        for (AnnotationInstance annotation : index.getAnnotations(REGISTER_PROVIDER)) {
            String targetClass = annotation.target().asClass().name().toString();
            producer.produce(new RegisterProviderAnnotationInstanceBuildItem(targetClass, annotation));
        }

        for (AnnotationInstance annotation : index.getAnnotations(REGISTER_PROVIDERS)) {
            String targetClass = annotation.target().asClass().name().toString();
            AnnotationInstance[] nestedArray = annotation.value().asNestedArray();
            if ((nestedArray != null) && nestedArray.length > 0) {
                for (AnnotationInstance nestedInstance : nestedArray) {
                    producer.produce(new RegisterProviderAnnotationInstanceBuildItem(targetClass, nestedInstance));
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
            List<RegisterProviderAnnotationInstanceBuildItem> registerProviderAnnotationInstances,
            List<AnnotationToRegisterIntoClientContextBuildItem> annotationsToRegisterIntoClientContext,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            RestClientReactiveConfig clientConfig) {
        String annotationRegisteredProvidersImpl = AnnotationRegisteredProviders.class.getName() + "Implementation";
        IndexView index = indexBuildItem.getIndex();
        Map<String, List<AnnotationInstance>> annotationsByClassName = new HashMap<>();

        for (RegisterProviderAnnotationInstanceBuildItem bi : registerProviderAnnotationInstances) {
            annotationsByClassName.computeIfAbsent(bi.getTargetClass(), key -> new ArrayList<>())
                    .add(bi.getAnnotationInstance());
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
                            .declaredAnnotation(ResteasyReactiveDotNames.CONSTRAINED_TO);
                    if (constrainedToInstance != null) {
                        if (RuntimeType.valueOf(constrainedToInstance.value().asEnum()) == RuntimeType.SERVER) {
                            continue;
                        }
                    }

                    if (skipAutoDiscoveredProvider(providerClass.interfaceNames())) {
                        continue;
                    }

                    DotName providerDotName = providerClass.name();
                    int priority = getAnnotatedPriority(index, providerDotName.toString(), Priorities.USER);

                    constructor.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AnnotationRegisteredProviders.class, "addGlobalProvider",
                                    void.class, Class.class,
                                    int.class),
                            constructor.getThis(), constructor.loadClassFromTCCL(providerDotName.toString()),
                            constructor.load(priority));
                }
            }

            MultivaluedMap<String, GeneratedClassResult> generatedProviders = new QuarkusMultivaluedHashMap<>();
            populateClientExceptionMapperFromAnnotations(generatedClasses, reflectiveClasses, index)
                    .forEach(generatedProviders::add);
            populateClientRedirectHandlerFromAnnotations(generatedClasses, reflectiveClasses, index)
                    .forEach(generatedProviders::add);
            for (AnnotationToRegisterIntoClientContextBuildItem annotation : annotationsToRegisterIntoClientContext) {
                populateClientProviderFromAnnotations(annotation, generatedClasses, reflectiveClasses, index)
                        .forEach(generatedProviders::add);

            }

            addGeneratedProviders(index, constructor, annotationsByClassName, generatedProviders);

            constructor.returnValue(null);
        }

        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(annotationRegisteredProvidersImpl));
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
    void registerCompressionInterceptors(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        Boolean enableCompression = ConfigProvider.getConfig()
                .getOptionalValue(ENABLE_COMPRESSION, Boolean.class)
                .orElse(false);
        if (enableCompression) {
            reflectiveClasses.produce(ReflectiveClassBuildItem
                    .builder(ClientGZIPDecodingInterceptor.class)
                    .serialization(false)
                    .build());
        }
    }

    @BuildStep
    void handleSseEventFilter(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem) {
        var index = beanArchiveIndexBuildItem.getIndex();
        Collection<AnnotationInstance> instances = index.getAnnotations(DotNames.SSE_EVENT_FILTER);
        if (instances.isEmpty()) {
            return;
        }

        List<String> filterClassNames = new ArrayList<>(instances.size());
        for (AnnotationInstance instance : instances) {
            if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            if (instance.value() == null) {
                continue; // can't happen
            }
            Type filterType = instance.value().asClass();
            DotName filterClassName = filterType.name();
            ClassInfo filterClassInfo = index.getClassByName(filterClassName.toString());
            if (filterClassInfo == null) {
                log.warn("Unable to find class '" + filterType.name() + "' in index");
            } else if (!filterClassInfo.hasNoArgsConstructor()) {
                throw new RestClientDefinitionException(
                        "Classes used in @SseEventFilter must have a no-args constructor. Offending class is '"
                                + filterClassName + "'");
            } else {
                filterClassNames.add(filterClassName.toString());
            }
        }
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder(filterClassNames.toArray(new String[0]))
                .constructors(true)
                .build());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void addRestClientBeans(Capabilities capabilities,
            CombinedIndexBuildItem combinedIndexBuildItem,
            CustomScopeAnnotationsBuildItem scopes,
            List<RestClientAnnotationsTransformerBuildItem> restClientAnnotationsTransformerBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            RestClientReactiveConfig clientConfig,
            RestClientsBuildTimeConfig clientsBuildConfig,
            RestClientRecorder recorder) {

        CompositeIndex index = CompositeIndex.create(combinedIndexBuildItem.getIndex());

        Set<AnnotationInstance> registerRestClientAnnos = determineRegisterRestClientInstances(clientsBuildConfig, index);

        Map<String, String> configKeys = new HashMap<>();
        var annotationsStore = new AnnotationStore(restClientAnnotationsTransformerBuildItem.stream()
                .map(RestClientAnnotationsTransformerBuildItem::getAnnotationsTransformer).collect(toList()));
        for (AnnotationInstance registerRestClient : registerRestClientAnnos) {
            ClassInfo jaxrsInterface = registerRestClient.target().asClass();
            // for each interface annotated with @RegisterRestClient, generate a $$CDIWrapper CDI bean that can be injected
            if (Modifier.isAbstract(jaxrsInterface.flags())) {
                validateKotlinDefaultMethods(jaxrsInterface, index);

                List<MethodInfo> methodsToImplement = new ArrayList<>();

                // search this interface and its super interfaces for jaxrs methods
                searchForJaxRsMethods(methodsToImplement, jaxrsInterface, index);
                // search this interface for default methods
                // we could search for default methods in super interfaces too,
                // but emitting the correct invokespecial instruction would become convoluted
                // (as invokespecial may only reference a method from a _direct_ super interface)
                for (MethodInfo method : jaxrsInterface.methods()) {
                    boolean isDefault = !Modifier.isAbstract(method.flags()) && !Modifier.isStatic(method.flags());
                    if (isDefault) {
                        methodsToImplement.add(method);
                    }
                }
                if (methodsToImplement.isEmpty()) {
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

                    configKey.ifPresent(
                            key -> configKeys.put(jaxrsInterface.name().toString(), key));

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

                    for (AnnotationInstance annotation : annotationsStore.getAnnotations(jaxrsInterface)) {
                        if (SKIP_COPYING_ANNOTATIONS_TO_GENERATED_CLASS.contains(annotation.name())) {
                            continue;
                        }

                        // scope annotation is added to the generated class already, see above
                        if (scopes.isScopeIn(Set.of(annotation))) {
                            continue;
                        }

                        classCreator.addAnnotation(annotation);
                    }

                    // CONSTRUCTOR:

                    MethodCreator constructor = classCreator
                            .getMethodCreator(MethodDescriptor.ofConstructor(classCreator.getClassName()));

                    AnnotationValue baseUri = registerRestClient.value("baseUri");

                    ResultHandle baseUriHandle = constructor.load(baseUri != null ? baseUri.asString() : "");
                    constructor.invokeSpecialMethod(
                            MethodDescriptor.ofConstructor(RestClientReactiveCDIWrapperBase.class, Class.class, String.class,
                                    String.class, boolean.class),
                            constructor.getThis(),
                            constructor.loadClassFromTCCL(jaxrsInterface.toString()),
                            baseUriHandle,
                            configKey.isPresent() ? constructor.load(configKey.get()) : constructor.loadNull(),
                            constructor.load(scope.getDotName().equals(REQUEST_SCOPED)));
                    constructor.returnValue(null);

                    // METHODS:
                    for (MethodInfo method : methodsToImplement) {
                        // for each method that corresponds to making a rest call, create a method like:
                        // public JsonArray get() {
                        //      return ((InterfaceClass)this.getDelegate()).get();
                        // }
                        //
                        // for each default method, create a method like:
                        // public JsonArray get() {
                        //     return InterfaceClass.super.get();
                        // }
                        MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.of(method));
                        methodCreator.setSignature(method.genericSignatureIfRequired());

                        // copy method annotations, there can be interceptors bound to them:
                        for (AnnotationInstance annotation : annotationsStore.getAnnotations(method)) {
                            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD
                                    && !BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.containsKey(annotation.name())
                                    && !ResteasyReactiveDotNames.PATH.equals(annotation.name())) {
                                methodCreator.addAnnotation(annotation);
                            }
                            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                                // TODO should skip annotations like `@PathParam` / `@RestPath`, probably (?)
                                short position = annotation.target().asMethodParameter().position();
                                methodCreator.getParameterAnnotations(position).addAnnotation(annotation);
                            }
                        }

                        ResultHandle result;

                        int parameterCount = method.parameterTypes().size();
                        ResultHandle[] params = new ResultHandle[parameterCount];
                        for (int i = 0; i < parameterCount; i++) {
                            params[i] = methodCreator.getMethodParam(i);
                        }

                        if (Modifier.isAbstract(method.flags())) { // RestClient method
                            ResultHandle delegate = methodCreator.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(RestClientReactiveCDIWrapperBase.class, "getDelegate",
                                            Object.class),
                                    methodCreator.getThis());

                            result = methodCreator.invokeInterfaceMethod(method, delegate, params);
                        } else { // default method
                            result = methodCreator.invokeSpecialInterfaceMethod(method, methodCreator.getThis(), params);
                        }

                        methodCreator.returnValue(result);
                    }
                }
            }
        }

        Set<String> blockingClassNames = new HashSet<>();
        Set<AnnotationInstance> registerBlockingClasses = new HashSet<>(index.getAnnotations(BLOCKING));
        for (AnnotationInstance registerBlockingClass : registerBlockingClasses) {
            AnnotationTarget target = registerBlockingClass.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS
                    && isImplementorOf(index, target.asClass(), RESPONSE_EXCEPTION_MAPPER, Set.of(APPLICATION))) {
                // Watch for @Blocking annotations in classes that implements ResponseExceptionMapper:
                blockingClassNames.add(target.asClass().toString());
            } else if (target.kind() == AnnotationTarget.Kind.METHOD
                    && target.asMethod().annotation(CLIENT_EXCEPTION_MAPPER) != null) {
                // Watch for @Blocking annotations in methods that are also annotated with @ClientExceptionMapper:
                blockingClassNames.add(ClientExceptionMapperHandler.getGeneratedClassName(target.asMethod()));
            }
        }

        recorder.setBlockingClassNames(blockingClassNames);

        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            recorder.setConfigKeys(configKeys);
        }
    }

    private Set<AnnotationInstance> determineRegisterRestClientInstances(RestClientsBuildTimeConfig clientsConfig,
            CompositeIndex index) {
        // these are the actual instances
        Set<AnnotationInstance> registerRestClientAnnos = new HashSet<>(index.getAnnotations(REGISTER_REST_CLIENT));
        // a set of the original target class
        Set<ClassInfo> registerRestClientTargets = registerRestClientAnnos.stream().map(ai -> ai.target().asClass()).collect(
                Collectors.toSet());

        // now we go through the keys and if any of them correspond to classes that don't have a @RegisterRestClient annotation, we fake that annotation
        Set<String> configKeyNames = clientsConfig.configs.keySet();
        for (String configKeyName : configKeyNames) {
            ClassInfo classInfo = index.getClassByName(configKeyName);
            if (classInfo == null) {
                continue;
            }
            if (registerRestClientTargets.contains(classInfo)) {
                continue;
            }
            Optional<String> cdiScope = clientsConfig.configs.get(configKeyName).scope;
            if (cdiScope.isEmpty()) {
                continue;
            }
            registerRestClientAnnos.add(AnnotationInstance.builder(REGISTER_REST_CLIENT).add("configKey", configKeyName)
                    .buildWithTarget(classInfo));
        }
        return registerRestClientAnnos;
    }

    /**
     * Based on a list of interfaces implemented by @Provider class, determine if registration
     * should be skipped or not. Server-specific types should be omitted unless implementation
     * of a <code>ClientRequestFilter</code> exists on the same class explicitly.
     * Features should always be omitted.
     */
    private boolean skipAutoDiscoveredProvider(List<DotName> providerInterfaceNames) {
        if (providerInterfaceNames.contains(ResteasyReactiveDotNames.FEATURE)) {
            return true;
        }
        if (providerInterfaceNames.contains(ResteasyReactiveDotNames.CONTAINER_REQUEST_FILTER)
                || providerInterfaceNames.contains(ResteasyReactiveDotNames.CONTAINER_RESPONSE_FILTER)
                || providerInterfaceNames.contains(ResteasyReactiveDotNames.EXCEPTION_MAPPER)) {
            if (providerInterfaceNames.contains(CLIENT_REQUEST_FILTER)
                    || providerInterfaceNames.contains(CLIENT_RESPONSE_FILTER)) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private Map<String, GeneratedClassResult> populateClientExceptionMapperFromAnnotations(
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses, IndexView index) {

        var result = new HashMap<String, GeneratedClassResult>();
        ClientExceptionMapperHandler clientExceptionMapperHandler = new ClientExceptionMapperHandler(
                new GeneratedClassGizmoAdaptor(generatedClasses, true));
        for (AnnotationInstance instance : index.getAnnotations(CLIENT_EXCEPTION_MAPPER)) {
            GeneratedClassResult classResult = clientExceptionMapperHandler.generateResponseExceptionMapper(instance);
            if (classResult == null) {
                continue;
            }
            if (result.containsKey(classResult.interfaceName)) {
                throw new IllegalStateException("Only a single instance of '" + CLIENT_EXCEPTION_MAPPER
                        + "' is allowed per REST Client interface. Offending class is '" + classResult.interfaceName + "'");
            }
            result.put(classResult.interfaceName, classResult);
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(classResult.generatedClassName)
                    .serialization(false).build());
        }
        return result;
    }

    private Map<String, GeneratedClassResult> populateClientRedirectHandlerFromAnnotations(
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses, IndexView index) {

        var result = new HashMap<String, GeneratedClassResult>();
        ClientRedirectHandler clientHandler = new ClientRedirectHandler(new GeneratedClassGizmoAdaptor(generatedClasses, true));
        for (AnnotationInstance instance : index.getAnnotations(CLIENT_REDIRECT_HANDLER)) {
            GeneratedClassResult classResult = clientHandler.generateResponseExceptionMapper(instance);
            if (classResult == null) {
                continue;
            }

            GeneratedClassResult existing = result.get(classResult.interfaceName);
            if (existing != null && existing.priority == classResult.priority) {
                throw new IllegalStateException("Only a single instance of '" + CLIENT_REDIRECT_HANDLER
                        + "' with the same priority is allowed per REST Client interface. "
                        + "Offending class is '" + classResult.interfaceName + "'");
            } else if (existing == null || existing.priority < classResult.priority) {
                result.put(classResult.interfaceName, classResult);
                reflectiveClasses.produce(ReflectiveClassBuildItem.builder(classResult.generatedClassName)
                        .serialization(false).build());
            }
        }
        return result;
    }

    private Map<String, GeneratedClassResult> populateClientProviderFromAnnotations(
            AnnotationToRegisterIntoClientContextBuildItem annotationBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses, IndexView index) {

        var result = new HashMap<String, GeneratedClassResult>();
        ClientContextResolverHandler handler = new ClientContextResolverHandler(annotationBuildItem.getAnnotation(),
                annotationBuildItem.getExpectedReturnType(),
                new GeneratedClassGizmoAdaptor(generatedClasses, true));
        for (AnnotationInstance instance : index.getAnnotations(annotationBuildItem.getAnnotation())) {
            GeneratedClassResult classResult = handler.generateContextResolver(instance);
            if (classResult == null) {
                continue;
            }
            if (result.containsKey(classResult.interfaceName)) {
                throw new IllegalStateException("Only a single instance of '" + annotationBuildItem.getAnnotation()
                        + "' is allowed per REST Client interface. Offending class is '" + classResult.interfaceName + "'");
            }
            result.put(classResult.interfaceName, classResult);
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(classResult.generatedClassName)
                    .serialization(false).build());
        }
        return result;
    }

    private void addGeneratedProviders(IndexView index, MethodCreator constructor,
            Map<String, List<AnnotationInstance>> annotationsByClassName,
            Map<String, List<GeneratedClassResult>> generatedProviders) {
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

                constructor.invokeInterfaceMethod(MAP_PUT, map, constructor.loadClassFromTCCL(className),
                        constructor.load(priority));
            }
            String ifaceName = annotationsForClass.getKey();
            if (generatedProviders.containsKey(ifaceName)) {
                // remove the interface from the generated provider since it's going to be handled now
                // the remaining entries will be handled later
                List<GeneratedClassResult> providers = generatedProviders.remove(ifaceName);
                for (GeneratedClassResult classResult : providers) {
                    constructor.invokeInterfaceMethod(MAP_PUT, map, constructor.loadClass(classResult.generatedClassName),
                            constructor.load(classResult.priority));
                }

            }
            addProviders(constructor, ifaceName, map);
        }

        for (Map.Entry<String, List<GeneratedClassResult>> entry : generatedProviders.entrySet()) {
            ResultHandle map = constructor.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
            for (GeneratedClassResult classResult : entry.getValue()) {
                constructor.invokeInterfaceMethod(MAP_PUT, map, constructor.loadClass(classResult.generatedClassName),
                        constructor.load(classResult.priority));
                addProviders(constructor, entry.getKey(), map);
            }

        }
    }

    private void addProviders(MethodCreator constructor, String providerClass, ResultHandle map) {
        constructor.invokeVirtualMethod(
                MethodDescriptor.ofMethod(AnnotationRegisteredProviders.class, "addProviders", void.class, String.class,
                        Map.class),
                constructor.getThis(), constructor.load(providerClass), map);
    }

    private int getAnnotatedPriority(IndexView index, String className, int defaultPriority) {
        ClassInfo providerClass = index.getClassByName(DotName.createSimple(className));
        int priority = defaultPriority;
        if (providerClass == null) {
            log.warnv("Unindexed provider class {0}. The priority of the provider will be set to {1}. ", className,
                    defaultPriority);
        } else {
            AnnotationInstance priorityAnnoOnProvider = providerClass.declaredAnnotation(ResteasyReactiveDotNames.PRIORITY);
            if (priorityAnnoOnProvider != null) {
                priority = priorityAnnoOnProvider.value().asInt();
            }
        }
        return priority;
    }

    // By default, Kotlin does not use Java interface default methods, but generates a helper class that contains the implementation.
    // In order to avoid the extra complexity of having to deal with this mode, we simply fail the build when this situation is encountered
    // and provide an actionable error message on how to remedy the situation.
    private void validateKotlinDefaultMethods(ClassInfo jaxrsInterface, IndexView index) {
        if (jaxrsInterface.declaredAnnotation(KOTLIN_METADATA_ANNOTATION) != null) {
            var potentialDefaultImplClass = DotName
                    .createSimple(jaxrsInterface.name().toString() + KOTLIN_INTERFACE_DEFAULT_IMPL_SUFFIX);
            if (index.getClassByName(potentialDefaultImplClass) != null) {
                throw new RestClientDefinitionException(String.format(
                        "Using Kotlin default methods on interfaces that are not backed by Java 8 default interface methods is not supported. See %s for more details. Offending interface is '%s'.",
                        "https://kotlinlang.org/docs/java-to-kotlin-interop.html#default-methods-in-interfaces",
                        jaxrsInterface.name().toString()));
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
            log.warnv("Unable to map the global rest client scope: '{0}' to a scope. Using @ApplicationScoped",
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
                scopeToUse = globalDefaultScope.getInfo();
            }
        } else {
            final Set<DotName> annotations = restClientInterface.annotationsMap().keySet();
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
