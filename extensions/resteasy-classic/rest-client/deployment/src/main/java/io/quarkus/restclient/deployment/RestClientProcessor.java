package io.quarkus.restclient.deployment;

import java.io.Closeable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Providers;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.annotation.RegisterProviders;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;
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
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.proxy.ProxyBuilderImpl;
import org.jboss.resteasy.client.jaxrs.internal.proxy.ResteasyClientProxy;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.microprofile.client.DefaultResponseExceptionMapper;
import org.jboss.resteasy.microprofile.client.RestClientProxy;
import org.jboss.resteasy.microprofile.client.async.AsyncInterceptorRxInvokerProvider;
import org.jboss.resteasy.microprofile.client.publisher.MpPublisherMessageBodyReader;
import org.jboss.resteasy.spi.ResteasyConfiguration;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
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
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.restclient.NoopHostnameVerifier;
import io.quarkus.restclient.runtime.PathFeatureHandler;
import io.quarkus.restclient.runtime.PathTemplateInjectionFilter;
import io.quarkus.restclient.runtime.RestClientBase;
import io.quarkus.restclient.runtime.RestClientRecorder;
import io.quarkus.resteasy.common.deployment.JaxrsProvidersToRegisterBuildItem;
import io.quarkus.resteasy.common.deployment.RestClientBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyDotNames;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;

class RestClientProcessor {
    private static final Logger log = Logger.getLogger(RestClientProcessor.class);

    private static final DotName REST_CLIENT = DotName.createSimple(RestClient.class.getName());
    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class.getName());

    private static final DotName SESSION_SCOPED = DotName.createSimple(SessionScoped.class.getName());

    private static final DotName PATH = DotName.createSimple(Path.class.getName());

    private static final DotName REGISTER_PROVIDER = DotName.createSimple(RegisterProvider.class.getName());
    private static final DotName REGISTER_PROVIDERS = DotName.createSimple(RegisterProviders.class.getName());
    private static final DotName REGISTER_CLIENT_HEADERS = DotName.createSimple(RegisterClientHeaders.class.getName());

    private static final DotName CLIENT_REQUEST_FILTER = DotName.createSimple(ClientRequestFilter.class.getName());
    private static final DotName CLIENT_RESPONSE_FILTER = DotName.createSimple(ClientResponseFilter.class.getName());
    private static final DotName CLIENT_HEADER_PARAM = DotName.createSimple(ClientHeaderParam.class.getName());

    private static final String PROVIDERS_SERVICE_FILE = "META-INF/services/" + Providers.class.getName();

    @BuildStep
    void setupProviders(BuildProducer<NativeImageResourceBuildItem> resources,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition) {

        proxyDefinition.produce(new NativeImageProxyDefinitionBuildItem("javax.ws.rs.ext.Providers"));
        resources.produce(new NativeImageResourceBuildItem(PROVIDERS_SERVICE_FILE));
    }

    @BuildStep
    void setupClientBuilder(BuildProducer<NativeImageResourceBuildItem> resources) {
        resources.produce(new NativeImageResourceBuildItem("META-INF/services/javax.ws.rs.client.ClientBuilder"));
    }

    @BuildStep
    NativeImageProxyDefinitionBuildItem addProxy() {
        return new NativeImageProxyDefinitionBuildItem(ResteasyConfiguration.class.getName());
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
                    .produce(new ReflectiveClassBuildItem(true, true, "io.smallrye.opentracing.SmallRyeRestClientListener"));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setup(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            RestClientRecorder restClientRecorder) {

        feature.produce(new FeatureBuildItem(Feature.REST_CLIENT));

        restClientRecorder.setRestClientBuilderResolver();

        additionalBeans.produce(new AdditionalBeanBuildItem(RestClient.class));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                DefaultResponseExceptionMapper.class.getName(),
                AsyncInterceptorRxInvokerProvider.class.getName(),
                ResteasyProviderFactoryImpl.class.getName(),
                ProxyBuilderImpl.class.getName(),
                ClientRequestFilter[].class.getName(),
                ClientResponseFilter[].class.getName(),
                javax.ws.rs.ext.ReaderInterceptor[].class.getName()));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                ResteasyClientBuilder.class.getName(), NoopHostnameVerifier.class.getName()));
    }

    @BuildStep
    void processInterfaces(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability,
            PackageConfig packageConfig,
            List<RestClientAnnotationProviderBuildItem> restClientAnnotationProviders,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<RestClientBuildItem> restClient) {

        // According to the spec only rest client interfaces annotated with RegisterRestClient are registered as beans
        Map<DotName, ClassInfo> interfaces = new HashMap<>();
        Set<Type> returnTypes = new HashSet<>();

        IndexView index = CompositeIndex.create(beanArchiveIndexBuildItem.getIndex(), combinedIndexBuildItem.getIndex());

        findInterfaces(index, interfaces, returnTypes, REGISTER_REST_CLIENT);
        findInterfaces(index, interfaces, returnTypes, PATH);

        if (interfaces.isEmpty()) {
            return;
        }

        for (DotName interfaze : interfaces.keySet()) {
            restClient.produce(new RestClientBuildItem(interfaze.toString()));
        }

        warnAboutNotWorkingFeaturesInNative(packageConfig, interfaces);

        for (Map.Entry<DotName, ClassInfo> entry : interfaces.entrySet()) {
            String iName = entry.getKey().toString();
            // the native image proxy definitions have to be separate because
            // MP REST Client impl creates a JDK proxy that delegates to a resteasy JDK proxy
            proxyDefinition.produce(new NativeImageProxyDefinitionBuildItem(iName, ResteasyClientProxy.class.getName()));
            proxyDefinition.produce(
                    new NativeImageProxyDefinitionBuildItem(iName, RestClientProxy.class.getName(), Closeable.class.getName()));
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, iName));
        }

        // Incoming headers
        // required for the non-arg constructor of DCHFImpl to be included in the native image
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, DefaultClientHeadersFactoryImpl.class.getName()));

        // Register Interface return types for reflection
        for (Type returnType : returnTypes) {
            reflectiveHierarchy
                    .produce(new ReflectiveHierarchyBuildItem.Builder()
                            .type(returnType)
                            .ignoreTypePredicate(ResteasyDotNames.IGNORE_TYPE_FOR_REFLECTION_PREDICATE)
                            .ignoreFieldPredicate(ResteasyDotNames.IGNORE_FIELD_FOR_REFLECTION_PREDICATE)
                            .ignoreMethodPredicate(ResteasyDotNames.IGNORE_METHOD_FOR_REFLECTION_PREDICATE)
                            .source(getClass().getSimpleName() + " > " + returnType.toString())
                            .build());
        }

        final Config config = ConfigProvider.getConfig();

        for (Map.Entry<DotName, ClassInfo> entry : interfaces.entrySet()) {
            DotName restClientName = entry.getKey();
            ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem.configure(restClientName);
            // The spec is not clear whether we should add superinterfaces too - let's keep aligned with SmallRye for now
            configurator.addType(restClientName);
            configurator.addQualifier(REST_CLIENT);
            final String configPrefix = computeConfigPrefix(restClientName.toString(), entry.getValue());
            final ScopeInfo scope = computeDefaultScope(capabilities, config, entry, configPrefix);
            final List<Class<?>> annotationProviders = checkAnnotationProviders(entry.getValue(),
                    restClientAnnotationProviders);
            configurator.scope(scope);
            configurator.creator(m -> {
                // return new RestClientBase(proxyType, baseUri).create();
                ResultHandle interfaceHandle = m.loadClass(restClientName.toString());
                ResultHandle baseUriHandle = m.load(getAnnotationParameter(entry.getValue(), "baseUri"));
                ResultHandle configPrefixHandle = m.load(configPrefix);
                ResultHandle annotationProvidersHandle = null;
                if (!annotationProviders.isEmpty()) {
                    annotationProvidersHandle = m.newArray(Class.class, annotationProviders.size());
                    for (int i = 0; i < annotationProviders.size(); i++) {
                        m.writeArrayValue(annotationProvidersHandle, i, m.loadClass(annotationProviders.get(i)));
                    }
                } else {
                    annotationProvidersHandle = m.loadNull();
                }
                ResultHandle baseHandle = m.newInstance(
                        MethodDescriptor.ofConstructor(RestClientBase.class, Class.class, String.class,
                                String.class,
                                Class[].class),
                        interfaceHandle, baseUriHandle, configPrefixHandle, annotationProvidersHandle);
                ResultHandle ret = m.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(RestClientBase.class, "create", Object.class), baseHandle);
                m.returnValue(ret);
            });
            configurator.destroyer(BeanDestroyer.CloseableDestroyer.class);

            syntheticBeans.produce(configurator.done());
        }
    }

    @BuildStep
    void clientTracingFeature(Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability, BuildProducer<ResteasyJaxrsProviderBuildItem> producer) {
        if (isRequired(capabilities, metricsCapability)) {
            producer.produce(new ResteasyJaxrsProviderBuildItem(PathFeatureHandler.class.getName()));
            producer.produce(new ResteasyJaxrsProviderBuildItem(PathTemplateInjectionFilter.class.getName()));
        }
    }

    private boolean isRequired(Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability) {
        return (capabilities.isPresent(Capability.OPENTELEMETRY_TRACER) ||
                (metricsCapability.isPresent()
                        && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)));
    }

    private static List<Class<?>> checkAnnotationProviders(ClassInfo classInfo,
            List<RestClientAnnotationProviderBuildItem> restClientAnnotationProviders) {
        return restClientAnnotationProviders.stream().filter(p -> (classInfo.classAnnotation(p.getAnnotationName()) != null))
                .map(p -> p.getProviderClass()).collect(Collectors.toList());
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(Feature.REST_CLIENT);
    }

    // currently default methods on a rest-client interface
    // that is annotated with ClientHeaderParam
    // leads to NPEs (see https://github.com/quarkusio/quarkus/issues/10249)
    // so lets warn users about its use
    private void warnAboutNotWorkingFeaturesInNative(PackageConfig packageConfig, Map<DotName, ClassInfo> interfaces) {
        if (!packageConfig.type.equalsIgnoreCase(PackageConfig.NATIVE)) {
            return;
        }
        Set<DotName> dotNames = new HashSet<>();
        for (ClassInfo interfaze : interfaces.values()) {
            if (interfaze.classAnnotation(CLIENT_HEADER_PARAM) != null) {
                boolean hasDefault = false;
                for (MethodInfo method : interfaze.methods()) {
                    if (isDefault(method.flags())) {
                        hasDefault = true;
                        break;
                    }
                }
                if (hasDefault) {
                    dotNames.add(interfaze.name());
                }
            }
        }
        if (!dotNames.isEmpty()) {
            log.warnf("rest-client interfaces that contain default methods and are annotated with '@" + CLIENT_HEADER_PARAM
                    + "' might not work properly in native mode. Offending interfaces are: "
                    + dotNames.stream().map(d -> "'" + d.toString() + "'").collect(Collectors.joining(", ")));
        }
    }

    private static boolean isDefault(short flags) {
        return ((flags & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC);
    }

    private void findInterfaces(IndexView index, Map<DotName, ClassInfo> interfaces, Set<Type> returnTypes,
            DotName annotationToFind) {
        for (AnnotationInstance annotation : index.getAnnotations(annotationToFind)) {
            AnnotationTarget target = annotation.target();
            ClassInfo theInfo;
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                theInfo = target.asClass();
            } else if (target.kind() == AnnotationTarget.Kind.METHOD) {
                theInfo = target.asMethod().declaringClass();
            } else {
                continue;
            }

            if (!isRestClientInterface(index, theInfo)) {
                continue;
            }

            interfaces.put(theInfo.name(), theInfo);

            // Find Return types
            processInterfaceReturnTypes(theInfo, returnTypes);
            for (Type interfaceType : theInfo.interfaceTypes()) {
                ClassInfo interfaceClassInfo = index.getClassByName(interfaceType.name());
                if (interfaceClassInfo != null) {
                    processInterfaceReturnTypes(interfaceClassInfo, returnTypes);
                }
            }
        }
    }

    private void processInterfaceReturnTypes(ClassInfo classInfo, Set<Type> returnTypes) {
        for (MethodInfo method : classInfo.methods()) {
            Type type = method.returnType();
            if (!type.name().toString().startsWith("java.lang")) {
                returnTypes.add(type);
            }
        }
    }

    private String computeConfigPrefix(String interfaceName, ClassInfo classInfo) {
        String propertyPrefixFromAnnotation = getAnnotationParameter(classInfo, "configKey");

        if (propertyPrefixFromAnnotation != null && !propertyPrefixFromAnnotation.isEmpty()) {
            return propertyPrefixFromAnnotation;
        }

        return interfaceName;
    }

    private ScopeInfo computeDefaultScope(Capabilities capabilities, Config config, Map.Entry<DotName, ClassInfo> entry,
            String configPrefix) {
        ScopeInfo scopeToUse = null;
        final Optional<String> scopeConfig = config
                .getOptionalValue(String.format(RestClientBase.REST_SCOPE_FORMAT, configPrefix), String.class);

        if (scopeConfig.isPresent()) {
            final DotName scope = DotName.createSimple(scopeConfig.get());
            final BuiltinScope builtinScope = BuiltinScope.from(scope);
            if (builtinScope != null) { // override default @Dependent scope with user defined one.
                scopeToUse = builtinScope.getInfo();
            } else if (capabilities.isPresent(Capability.SERVLET)) {
                if (scope.equals(SESSION_SCOPED)) {
                    scopeToUse = new ScopeInfo(SESSION_SCOPED, true);
                }
            }

            if (scopeToUse == null) {
                log.warn(String.format(
                        "Unsupported default scope %s provided for rest client %s. Defaulting to @Dependent.",
                        scope, entry.getKey()));
                scopeToUse = BuiltinScope.DEPENDENT.getInfo();
            }
        } else {
            final Set<DotName> annotations = entry.getValue().annotations().keySet();
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
        return scopeToUse != null ? scopeToUse : BuiltinScope.DEPENDENT.getInfo();
    }

    private String getAnnotationParameter(ClassInfo classInfo, String parameterName) {
        AnnotationInstance instance = classInfo.classAnnotation(REGISTER_REST_CLIENT);
        if (instance == null) {
            return "";
        }

        AnnotationValue value = instance.value(parameterName);
        if (value == null) {
            return "";
        }

        return value.asString();
    }

    @BuildStep
    IgnoreClientProviderBuildItem ignoreMPPublisher() {
        // hack to remove a provider that is manually registered QuarkusRestClientBuilder
        return new IgnoreClientProviderBuildItem(MpPublisherMessageBodyReader.class.getName());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerProviders(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            JaxrsProvidersToRegisterBuildItem jaxrsProvidersToRegisterBuildItem,
            List<IgnoreClientProviderBuildItem> ignoreClientProviderBuildItems,
            CombinedIndexBuildItem combinedIndexBuildItem,
            ResteasyInjectionReadyBuildItem injectorFactory,
            RestClientRecorder restClientRecorder) {

        for (IgnoreClientProviderBuildItem item : ignoreClientProviderBuildItems) {
            jaxrsProvidersToRegisterBuildItem.getProviders().remove(item.getProviderClassName());
            jaxrsProvidersToRegisterBuildItem.getContributedProviders().remove(item.getProviderClassName());
        }

        restClientRecorder.initializeResteasyProviderFactory(injectorFactory.getInjectorFactory(),
                jaxrsProvidersToRegisterBuildItem.useBuiltIn(),
                jaxrsProvidersToRegisterBuildItem.getProviders(), jaxrsProvidersToRegisterBuildItem.getContributedProviders());

        // register the providers for reflection
        for (String providerToRegister : jaxrsProvidersToRegisterBuildItem.getProviders()) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, providerToRegister));
        }

        // now we register all values of @RegisterProvider for constructor reflection

        IndexView index = combinedIndexBuildItem.getIndex();
        List<AnnotationInstance> allInstances = new ArrayList<>(index.getAnnotations(REGISTER_PROVIDER));
        for (AnnotationInstance annotation : index.getAnnotations(REGISTER_PROVIDERS)) {
            allInstances.addAll(Arrays.asList(annotation.value().asNestedArray()));
        }
        for (AnnotationInstance annotationInstance : allInstances) {
            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(false, false, annotationInstance.value().asClass().toString()));
        }

        // Register @RegisterClientHeaders for reflection
        for (AnnotationInstance annotationInstance : index.getAnnotations(REGISTER_CLIENT_HEADERS)) {
            AnnotationValue value = annotationInstance.value();
            if (value != null) {
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(false, false, annotationInstance.value().asClass().toString()));
            }
        }

        // now retain all un-annotated implementations of ClientRequestFilter and ClientResponseFilter
        // in case they are programmatically registered by applications
        for (ClassInfo info : index.getAllKnownImplementors(CLIENT_REQUEST_FILTER)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, info.name().toString()));
        }
        for (ClassInfo info : index.getAllKnownImplementors(CLIENT_RESPONSE_FILTER)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, info.name().toString()));
        }
    }

    @BuildStep
    AdditionalBeanBuildItem registerProviderBeans(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();
        List<AnnotationInstance> allInstances = new ArrayList<>(index.getAnnotations(REGISTER_PROVIDER));
        for (AnnotationInstance annotation : index.getAnnotations(REGISTER_PROVIDERS)) {
            allInstances.addAll(Arrays.asList(annotation.value().asNestedArray()));
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

    private boolean isRestClientInterface(IndexView index, ClassInfo classInfo) {
        return Modifier.isInterface(classInfo.flags())
                && index.getAllKnownImplementors(classInfo.name()).isEmpty();
    }
}
