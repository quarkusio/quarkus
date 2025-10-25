package io.quarkus.rest.client.reactive.deployment;

import static io.quarkus.jaxrs.client.reactive.deployment.MethodDescriptors.MAP_PUT;
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
import static io.quarkus.rest.client.reactive.deployment.RegisteredRestClientBuildItem.toRegisteredRestClients;
import static java.util.Arrays.asList;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Typed;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
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
import org.jboss.resteasy.reactive.client.impl.RestClientClosingTask;
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
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.jaxrs.client.reactive.deployment.JaxrsClientReactiveEnricherBuildItem;
import io.quarkus.jaxrs.client.reactive.deployment.RestClientDefaultConsumesBuildItem;
import io.quarkus.jaxrs.client.reactive.deployment.RestClientDefaultProducesBuildItem;
import io.quarkus.jaxrs.client.reactive.deployment.RestClientDisableRemovalTrailingSlashBuildItem;
import io.quarkus.jaxrs.client.reactive.deployment.RestClientDisableSmartDefaultProduces;
import io.quarkus.rest.client.reactive.CertificateUpdateEventListener;
import io.quarkus.rest.client.reactive.runtime.AnnotationRegisteredProviders;
import io.quarkus.rest.client.reactive.runtime.RestClientReactiveCDIWrapperBase;
import io.quarkus.rest.client.reactive.runtime.RestClientReactiveConfig;
import io.quarkus.rest.client.reactive.runtime.RestClientRecorder;
import io.quarkus.rest.client.reactive.spi.RestClientAnnotationsTransformerBuildItem;
import io.quarkus.restclient.config.RegisteredRestClient;
import io.quarkus.restclient.config.RestClientsBuildTimeConfig;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.restclient.config.deployment.RestClientConfigUtils;
import io.quarkus.restclient.config.deployment.RestClientsBuildTimeConfigBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.runtime.LaunchMode;

class RestClientReactiveProcessor {

    private static final Logger log = Logger.getLogger(RestClientReactiveProcessor.class);

    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class.getName());
    private static final DotName REST_CLIENT = DotName.createSimple(RestClient.class.getName());
    private static final DotName INJECT_MOCK = DotName.createSimple("io.quarkus.test.InjectMock");
    private static final DotName KOTLIN_METADATA_ANNOTATION = DotName.createSimple("kotlin.Metadata");

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
        features.produce(new FeatureBuildItem(Feature.REST_CLIENT));
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(Feature.REST_CLIENT);
    }

    @BuildStep
    void nativeSpiSupport(BuildProducer<ServiceProviderBuildItem> producer) {
        producer.produce(ServiceProviderBuildItem
                .allProvidersFromClassPath(MissingMessageBodyReaderErrorMessageContextualizer.class.getName()));
        producer.produce(ServiceProviderBuildItem
                .allProvidersFromClassPath(RestClientClosingTask.class.getName()));
    }

    @BuildStep
    void setUpClientBuildTimeProperties(BuildProducer<RestClientDefaultConsumesBuildItem> consumes,
            BuildProducer<RestClientDefaultProducesBuildItem> produces,
            BuildProducer<RestClientDisableSmartDefaultProduces> disableSmartProduces,
            BuildProducer<RestClientDisableRemovalTrailingSlashBuildItem> disableRemovalTrailingSlash,
            RestClientReactiveConfig restClientReactiveConfig,
            List<RegisteredRestClientBuildItem> registeredRestClientBuildItems,
            ResourceScanningResultBuildItem resourceScanningResultBuildItem,
            BuildProducer<RestClientsBuildTimeConfigBuildItem> restClientBuildTimeConfig) {

        consumes.produce(new RestClientDefaultConsumesBuildItem(MediaType.APPLICATION_JSON, 10));
        produces.produce(new RestClientDefaultProducesBuildItem(MediaType.APPLICATION_JSON, 10));
        if (restClientReactiveConfig.disableSmartProduces()) {
            disableSmartProduces.produce(new RestClientDisableSmartDefaultProduces());
        }

        List<RegisteredRestClient> registeredRestClients = new ArrayList<>(
                toRegisteredRestClients(registeredRestClientBuildItems));
        resourceScanningResultBuildItem.getResult().getClientInterfaces().forEach((restClient, path) -> {
            if (registeredRestClients.stream()
                    .noneMatch(registeredRestClient -> registeredRestClient.getFullName().equals(restClient.toString()))) {
                registeredRestClients.add(new RegisteredRestClient(restClient.toString(), restClient.withoutPackagePrefix()));
            }
        });
        RestClientsBuildTimeConfigBuildItem restClientsBuildTimeConfigBuildItem = new RestClientsBuildTimeConfigBuildItem(
                registeredRestClients);
        restClientBuildTimeConfig.produce(restClientsBuildTimeConfigBuildItem);

        List<DotName> clientsToDisable = new ArrayList<>();
        for (RegisteredRestClientBuildItem registeredRestClient : registeredRestClientBuildItems) {
            if (removesTrailingSlashIsDisabled(restClientsBuildTimeConfigBuildItem.getRestClientsBuildTimeConfig(),
                    registeredRestClient)) {
                clientsToDisable.add(registeredRestClient.getClassInfo().name());
            }
        }

        if (!clientsToDisable.isEmpty()) {
            disableRemovalTrailingSlash.produce(new RestClientDisableRemovalTrailingSlashBuildItem(clientsToDisable));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            RestClientRecorder restClientRecorder) {
        restClientRecorder.setRestClientBuilderResolver();
        additionalBeans.produce(new AdditionalBeanBuildItem(RestClient.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(CertificateUpdateEventListener.class));
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(RestClientsConfig.class, ClientLogger.class);
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
     */
    @BuildStep
    void registerProvidersFromAnnotations(CombinedIndexBuildItem indexBuildItem,
            List<RegisterProviderAnnotationInstanceBuildItem> registerProviderAnnotationInstances,
            List<AnnotationToRegisterIntoClientContextBuildItem> annotationsToRegisterIntoClientContext,
            BuildProducer<GeneratedBeanBuildItem> generatedBeansProducer,
            BuildProducer<GeneratedClassBuildItem> generatedClassesProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassesProducer,
            BuildProducer<ExecutionModelAnnotationsAllowedBuildItem> executionModelAnnotationsAllowedProducer,
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
                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeansProducer))
                .superClass(AnnotationRegisteredProviders.class)
                .build()) {

            classCreator.addAnnotation(Singleton.class.getName());
            MethodCreator constructor = classCreator
                    .getMethodCreator(MethodDescriptor.ofConstructor(annotationRegisteredProvidersImpl));
            constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(AnnotationRegisteredProviders.class),
                    constructor.getThis());

            if (clientConfig.providerAutodiscovery()) {
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

                    // when the server is not included, providers are not automatically registered for reflection,
                    // so we need to always do it for the client to be on the safe side
                    reflectiveClassesProducer.produce(ReflectiveClassBuildItem.builder(providerDotName.toString()).build());
                }
            }

            MultivaluedMap<String, GeneratedClassResult> generatedProviders = new QuarkusMultivaluedHashMap<>();
            populateClientExceptionMapperFromAnnotations(index, generatedClassesProducer, reflectiveClassesProducer,
                    executionModelAnnotationsAllowedProducer)
                    .forEach(generatedProviders::add);
            populateClientRedirectHandlerFromAnnotations(generatedClassesProducer, reflectiveClassesProducer, index)
                    .forEach(generatedProviders::add);
            for (AnnotationToRegisterIntoClientContextBuildItem annotation : annotationsToRegisterIntoClientContext) {
                populateClientProviderFromAnnotations(annotation, generatedClassesProducer, reflectiveClassesProducer, index)
                        .forEach(generatedProviders::add);

            }

            addGeneratedProviders(index, classCreator, constructor, annotationsByClassName, generatedProviders);

            constructor.returnValue(null);
        }

        unremovableBeansProducer.produce(UnremovableBeanBuildItem.beanClassNames(annotationRegisteredProvidersImpl));
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
                builder.addBeanClass(value.asClass().name().toString());
            }
        }
        return builder.build();
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
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(filterClassNames.toArray(new String[0]))
                .reason(getClass().getName())
                .build());
    }

    @BuildStep
    void determineRegisteredRestClients(
            CombinedIndexBuildItem combinedIndexBuildItem,
            RestClientsBuildTimeConfig clientsConfig,
            BuildProducer<RegisteredRestClientBuildItem> producer) {
        CompositeIndex index = CompositeIndex.create(combinedIndexBuildItem.getIndex());
        Set<DotName> seen = new HashSet<>();

        List<AnnotationInstance> actualInstances = index.getAnnotations(REGISTER_REST_CLIENT);
        for (AnnotationInstance instance : actualInstances) {
            AnnotationTarget annotationTarget = instance.target();
            ClassInfo classInfo = annotationTarget.asClass();
            if (!Modifier.isAbstract(classInfo.flags())) {
                continue;
            }
            DotName className = classInfo.name();
            seen.add(className);

            AnnotationValue configKeyValue = instance.value("configKey");
            Optional<String> configKey = configKeyValue == null ? Optional.empty() : Optional.of(configKeyValue.asString());

            AnnotationValue baseUriValue = instance.value("baseUri");
            Optional<String> baseUri = baseUriValue == null ? Optional.empty() : Optional.of(baseUriValue.asString());

            producer.produce(new RegisteredRestClientBuildItem(classInfo, configKey, baseUri));
        }

        // now we go through the keys and if any of them correspond to classes that don't have a @RegisterRestClient annotation, we fake that annotation
        Set<String> configKeyNames = clientsConfig.clients().keySet();
        for (String configKeyName : configKeyNames) {
            ClassInfo classInfo = index.getClassByName(configKeyName);
            if (classInfo == null) {
                continue;
            }
            if (seen.contains(classInfo.name())) {
                continue;
            }
            if (!Modifier.isAbstract(classInfo.flags())) {
                continue;
            }
            Optional<String> cdiScope = clientsConfig.clients().get(configKeyName).scope();
            if (cdiScope.isEmpty()) {
                continue;
            }
            producer.produce(new RegisteredRestClientBuildItem(classInfo, Optional.empty(), Optional.empty()));
        }
    }

    @BuildStep
    void generateRestClientConfigBuilder(
            List<RegisteredRestClientBuildItem> restClients,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<StaticInitConfigBuilderBuildItem> staticInitConfigBuilder,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {

        RestClientConfigUtils.generateRestClientConfigBuilder(toRegisteredRestClients(restClients), generatedClass,
                staticInitConfigBuilder, runTimeConfigBuilder);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void addRestClientBeans(
            Capabilities capabilities,
            CombinedIndexBuildItem combinedIndexBuildItem,
            RestClientsBuildTimeConfigBuildItem restClientsBuildTimeConfig,
            List<RegisteredRestClientBuildItem> registeredRestClients,
            CustomScopeAnnotationsBuildItem scopes,
            List<RestClientAnnotationsTransformerBuildItem> restClientAnnotationsTransformerBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            LaunchModeBuildItem launchMode,
            RestClientRecorder recorder,
            ShutdownContextBuildItem shutdown) {

        CompositeIndex index = CompositeIndex.create(combinedIndexBuildItem.getIndex());

        Set<DotName> requestedRestClientMocks = Collections.emptySet();
        if (launchMode.getLaunchMode() == LaunchMode.TEST) {
            // we need to determine which RestClient interfaces have been marked for mocking
            requestedRestClientMocks = combinedIndexBuildItem.getIndex().getAnnotations(INJECT_MOCK)
                    .stream()
                    .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.FIELD)
                    .map(ai -> ai.target().asField())
                    .filter(f -> f.hasAnnotation(REST_CLIENT))
                    .map(f -> f.type().name())
                    .collect(Collectors.toSet());
        }

        Map<String, String> configKeys = new HashMap<>();
        var annotationsStore = new AnnotationStore(index, restClientAnnotationsTransformerBuildItem.stream()
                .map(RestClientAnnotationsTransformerBuildItem::getAnnotationTransformation).toList());
        for (RegisteredRestClientBuildItem registerRestClient : registeredRestClients) {
            ClassInfo jaxrsInterface = registerRestClient.getClassInfo();
            // for each interface annotated with @RegisterRestClient, generate a $$CDIWrapper CDI bean that can be injected
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
                final Optional<String> configKey = registerRestClient.getConfigKey();

                configKey.ifPresent(
                        key -> configKeys.put(jaxrsInterface.name().toString(), key));

                final ScopeInfo scope = restClientsBuildTimeConfig.getScope(capabilities, jaxrsInterface)
                        .orElse(BuiltinScope.APPLICATION).getInfo();
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

                Optional<String> baseUri = registerRestClient.getDefaultBaseUri();

                ResultHandle baseUriHandle = constructor.load(baseUri.isPresent() ? baseUri.get() : "");
                boolean lazyDelegate = scope.getDotName().equals(REQUEST_SCOPED)
                        || requestedRestClientMocks.contains(jaxrsInterface.name());
                constructor.invokeSpecialMethod(
                        MethodDescriptor.ofConstructor(RestClientReactiveCDIWrapperBase.class, Class.class, String.class,
                                String.class, boolean.class),
                        constructor.getThis(),
                        constructor.loadClassFromTCCL(jaxrsInterface.toString()),
                        baseUriHandle,
                        configKey.isPresent() ? constructor.load(configKey.get()) : constructor.loadNull(),
                        constructor.load(lazyDelegate));
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
                    for (Type exception : method.exceptions()) {
                        // declared exceptions are important in case the client is intercepted, because interception
                        // subclasses wrap undeclared checked exceptions into `ArcUndeclaredThrowableException`
                        methodCreator.addException(exception.name().toString());
                    }
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

        recorder.cleanUp(shutdown);
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
            IndexView index,
            BuildProducer<GeneratedClassBuildItem> generatedClassesProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassesProducer,
            BuildProducer<ExecutionModelAnnotationsAllowedBuildItem> executionModelAnnotationsAllowedProducer) {

        executionModelAnnotationsAllowedProducer.produce(new ExecutionModelAnnotationsAllowedBuildItem(
                new Predicate<>() {
                    @Override
                    public boolean test(MethodInfo methodInfo) {
                        return methodInfo.hasDeclaredAnnotation(CLIENT_EXCEPTION_MAPPER);
                    }
                }));

        var result = new HashMap<String, GeneratedClassResult>();
        ClientExceptionMapperHandler clientExceptionMapperHandler = new ClientExceptionMapperHandler(
                new GeneratedClassGizmoAdaptor(generatedClassesProducer, true));
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
            reflectiveClassesProducer.produce(ReflectiveClassBuildItem.builder(classResult.generatedClassName)
                    .reason(getClass().getName())
                    .build());
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
                        .reason(getClass().getName())
                        .build());
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
                    .reason(getClass().getName())
                    .build());
        }
        return result;
    }

    private void addGeneratedProviders(IndexView index, ClassCreator classCreator, MethodCreator constructor,
            Map<String, List<AnnotationInstance>> annotationsByClassName,
            Map<String, List<GeneratedClassResult>> generatedProviders) {
        int i = 1;
        for (Map.Entry<String, List<AnnotationInstance>> annotationsForClass : annotationsByClassName.entrySet()) {
            MethodCreator mc = classCreator.getMethodCreator("addGeneratedProviders" + i, void.class);
            ResultHandle map = mc.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
            for (AnnotationInstance value : annotationsForClass.getValue()) {
                String className = value.value().asString();
                AnnotationValue priorityAnnotationValue = value.value("priority");
                int priority;
                if (priorityAnnotationValue == null) {
                    priority = getAnnotatedPriority(index, className, Priorities.USER);
                } else {
                    priority = priorityAnnotationValue.asInt();
                }

                mc.invokeInterfaceMethod(MAP_PUT, map, mc.loadClassFromTCCL(className),
                        mc.load(priority));
            }
            String ifaceName = annotationsForClass.getKey();
            if (generatedProviders.containsKey(ifaceName)) {
                // remove the interface from the generated provider since it's going to be handled now
                // the remaining entries will be handled later
                List<GeneratedClassResult> providers = generatedProviders.remove(ifaceName);
                for (GeneratedClassResult classResult : providers) {
                    mc.invokeInterfaceMethod(MAP_PUT, map, mc.loadClass(classResult.generatedClassName),
                            mc.load(classResult.priority));
                }

            }
            addProviders(mc, ifaceName, map);
            mc.returnVoid();

            constructor.invokeVirtualMethod(mc.getMethodDescriptor(), constructor.getThis());

            i++;
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

    private void addProviders(MethodCreator mc, String providerClass, ResultHandle map) {
        mc.invokeVirtualMethod(
                MethodDescriptor.ofMethod(AnnotationRegisteredProviders.class, "addProviders", void.class, String.class,
                        Map.class),
                mc.getThis(), mc.load(providerClass), map);
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

    private boolean removesTrailingSlashIsDisabled(RestClientsBuildTimeConfig config,
            RegisteredRestClientBuildItem registeredRestClient) {
        // is disabled for all the clients
        if (!config.removesTrailingSlash()) {
            return true;
        }

        // is disabled for this concrete client
        return !config.clients()
                .get(registeredRestClient.getClassInfo().name().toString())
                .removesTrailingSlash();
    }
}
