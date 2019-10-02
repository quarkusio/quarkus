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

import javax.ws.rs.Path;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Providers;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.annotation.RegisterProviders;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
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
import org.jboss.resteasy.spi.ResteasyConfiguration;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrarBuildItem;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.ScopeInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.restclient.runtime.IncomingHeadersProvider;
import io.quarkus.restclient.runtime.RestClientBase;
import io.quarkus.restclient.runtime.RestClientRecorder;
import io.quarkus.resteasy.common.deployment.JaxrsProvidersToRegisterBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyDotNames;
import io.quarkus.resteasy.common.deployment.ResteasyInjectionReadyBuildItem;

class RestClientProcessor {
    private static final Logger log = Logger.getLogger(RestClientProcessor.class);

    private static final DotName REST_CLIENT = DotName.createSimple(RestClient.class.getName());
    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class.getName());

    private static final DotName PATH = DotName.createSimple(Path.class.getName());

    private static final DotName REGISTER_PROVIDER = DotName.createSimple(RegisterProvider.class.getName());
    private static final DotName REGISTER_PROVIDERS = DotName.createSimple(RegisterProviders.class.getName());

    private static final String PROVIDERS_SERVICE_FILE = "META-INF/services/" + Providers.class.getName();

    @BuildStep
    void setupProviders(BuildProducer<SubstrateResourceBuildItem> resources,
            BuildProducer<SubstrateProxyDefinitionBuildItem> proxyDefinition) {

        proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem("javax.ws.rs.ext.Providers"));
        resources.produce(new SubstrateResourceBuildItem(PROVIDERS_SERVICE_FILE));
    }

    @BuildStep
    SubstrateProxyDefinitionBuildItem addProxy() {
        return new SubstrateProxyDefinitionBuildItem(ResteasyConfiguration.class.getName());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setup(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            RestClientRecorder restClientRecorder) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.REST_CLIENT));

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
                ResteasyClientBuilder.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void processInterfaces(CombinedIndexBuildItem combinedIndexBuildItem,
            SslNativeConfigBuildItem sslNativeConfig,
            BuildProducer<SubstrateProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<BeanRegistrarBuildItem> beanRegistrars,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            RestClientRecorder restClientRecorder) {

        // According to the spec only rest client interfaces annotated with RegisterRestClient are registered as beans
        Map<DotName, ClassInfo> interfaces = new HashMap<>();
        Set<Type> returnTypes = new HashSet<>();

        IndexView index = combinedIndexBuildItem.getIndex();
        for (AnnotationInstance annotation : index.getAnnotations(PATH)) {
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
            for (MethodInfo method : theInfo.methods()) {
                Type type = method.returnType();
                if (!type.name().toString().contains("java.lang")) {
                    if (!returnTypes.contains(type)) {
                        returnTypes.add(type);
                    }
                }
            }
        }

        if (interfaces.isEmpty()) {
            return;
        }

        for (Map.Entry<DotName, ClassInfo> entry : interfaces.entrySet()) {
            String iName = entry.getKey().toString();
            // the SubstrateProxyDefinitions have to be separate because
            // MP REST Client impl creates a JDK proxy that delegates to a resteasy JDK proxy
            proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem(iName, ResteasyClientProxy.class.getName()));
            proxyDefinition.produce(
                    new SubstrateProxyDefinitionBuildItem(iName, RestClientProxy.class.getName(), Closeable.class.getName()));
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, iName));
        }

        // Incoming headers
        // required for the non-arg constructor of DCHFImpl to be included in the native image
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, DefaultClientHeadersFactoryImpl.class.getName()));
        serviceProvider
                .produce(new ServiceProviderBuildItem(
                        org.jboss.resteasy.microprofile.client.header.IncomingHeadersProvider.class.getName(),
                        IncomingHeadersProvider.class.getName()));

        // Register Interface return types for reflection
        for (Type returnType : returnTypes) {
            if (isReflectionDeclarationRequiredFor(returnType)) {
                reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(returnType));
            }
        }

        beanRegistrars.produce(new BeanRegistrarBuildItem(new BeanRegistrar() {

            @Override
            public void register(RegistrationContext registrationContext) {
                final Config config = ConfigProvider.getConfig();

                for (Map.Entry<DotName, ClassInfo> entry : interfaces.entrySet()) {
                    DotName restClientName = entry.getKey();
                    BeanConfigurator<Object> configurator = registrationContext.configure(restClientName);
                    // The spec is not clear whether we should add superinterfaces too - let's keep aligned with SmallRye for now
                    configurator.addType(restClientName);
                    configurator.addQualifier(REST_CLIENT);
                    final ScopeInfo scope = computeDefaultScope(config, entry);
                    configurator.scope(scope);
                    configurator.creator(m -> {
                        // return new RestClientBase(proxyType, baseUri).create();
                        ResultHandle interfaceHandle = m.loadClass(restClientName.toString());
                        ResultHandle baseUriHandle = m.load(getBaseUri(entry.getValue()));
                        ResultHandle baseHandle = m.newInstance(
                                MethodDescriptor.ofConstructor(RestClientBase.class, Class.class, String.class),
                                interfaceHandle, baseUriHandle);
                        ResultHandle ret = m.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(RestClientBase.class, "create", Object.class), baseHandle);
                        m.returnValue(ret);
                    });
                    configurator.done();
                }
            }
        }));

        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.REST_CLIENT));

        restClientRecorder.setSslEnabled(sslNativeConfig.isEnabled());
    }

    private ScopeInfo computeDefaultScope(Config config, Map.Entry<DotName, ClassInfo> entry) {
        DotName restClientName = entry.getKey();
        // Initialize a default @Dependent scope as per the spec
        ScopeInfo scopeInfo = BuiltinScope.DEPENDENT.getInfo();
        final String REST_SCOPE_FORMAT = "%s/" + RestClientBase.MP_REST + "/scope";
        final Optional<String> scopeConfig = config
                .getOptionalValue(String.format(REST_SCOPE_FORMAT, restClientName.toString()), String.class);
        if (scopeConfig.isPresent()) {
            final DotName scope = DotName.createSimple(scopeConfig.get());
            final BuiltinScope builtinScope = BuiltinScope.from(scope);
            if (builtinScope != null) { // override default @Dependent scope with user defined one.
                scopeInfo = builtinScope.getInfo();
            } else {
                log.warn(String.format(
                        "Unsupported default scope %s provided for rest client %s. Defaulting to @Dependent.",
                        scope, restClientName));
            }
        } else {
            final Set<DotName> annotations = entry.getValue().annotations().keySet();
            for (final DotName annotationName : annotations) {
                final BuiltinScope builtinScope = BuiltinScope.from(annotationName);
                if (builtinScope != null) {
                    scopeInfo = builtinScope.getInfo();
                    break;
                }
            }
        }

        return scopeInfo;
    }

    private String getBaseUri(ClassInfo classInfo) {
        AnnotationInstance instance = classInfo.classAnnotation(REGISTER_REST_CLIENT);
        if (instance == null) {
            return "";
        }

        AnnotationValue value = instance.value("baseUri");
        if (value == null) {
            return "";
        }

        return value.asString();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerProviders(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            JaxrsProvidersToRegisterBuildItem jaxrsProvidersToRegisterBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            ResteasyInjectionReadyBuildItem injectorFactory,
            RestClientRecorder restClientRecorder) {

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
    }

    private boolean isRestClientInterface(IndexView index, ClassInfo classInfo) {
        return Modifier.isInterface(classInfo.flags())
                && index.getAllKnownImplementors(classInfo.name()).isEmpty();
    }

    private static boolean isReflectionDeclarationRequiredFor(Type type) {
        DotName className = getClassName(type);

        return className != null && !ResteasyDotNames.TYPES_IGNORED_FOR_REFLECTION.contains(className);
    }

    private static DotName getClassName(Type type) {
        switch (type.kind()) {
            case CLASS:
            case PARAMETERIZED_TYPE:
                return type.name();
            case ARRAY:
                return getClassName(type.asArrayType().component());
            default:
                return null;
        }
    }
}
