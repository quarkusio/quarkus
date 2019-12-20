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

import javax.enterprise.context.SessionScoped;
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
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.restclient.runtime.RestClientBase;
import io.quarkus.restclient.runtime.RestClientRecorder;
import io.quarkus.resteasy.common.deployment.JaxrsProvidersToRegisterBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyDotNames;
import io.quarkus.resteasy.common.deployment.ResteasyInjectionReadyBuildItem;

class RestClientProcessor {
    private static final Logger log = Logger.getLogger(RestClientProcessor.class);

    private static final DotName REST_CLIENT = DotName.createSimple(RestClient.class.getName());
    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class.getName());

    private static final DotName SESSION_SCOPED = DotName.createSimple(SessionScoped.class.getName());

    private static final DotName PATH = DotName.createSimple(Path.class.getName());

    private static final DotName REGISTER_PROVIDER = DotName.createSimple(RegisterProvider.class.getName());
    private static final DotName REGISTER_PROVIDERS = DotName.createSimple(RegisterProviders.class.getName());

    private static final String PROVIDERS_SERVICE_FILE = "META-INF/services/" + Providers.class.getName();

    @BuildStep
    void setupProviders(BuildProducer<NativeImageResourceBuildItem> resources,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition) {

        proxyDefinition.produce(new NativeImageProxyDefinitionBuildItem("javax.ws.rs.ext.Providers"));
        resources.produce(new NativeImageResourceBuildItem(PROVIDERS_SERVICE_FILE));
    }

    @BuildStep
    NativeImageProxyDefinitionBuildItem addProxy() {
        return new NativeImageProxyDefinitionBuildItem(ResteasyConfiguration.class.getName());
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
            Capabilities capabilities,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition,
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

        findInterfaces(index, interfaces, returnTypes, REGISTER_REST_CLIENT);
        findInterfaces(index, interfaces, returnTypes, PATH);

        if (interfaces.isEmpty()) {
            return;
        }

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
                    .produce(new ReflectiveHierarchyBuildItem(returnType, ResteasyDotNames.IGNORE_FOR_REFLECTION_PREDICATE));
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
                    final String configPrefix = computeConfigPrefix(restClientName.toString(), entry.getValue());
                    final ScopeInfo scope = computeDefaultScope(capabilities, config, entry, configPrefix);
                    configurator.scope(scope);
                    configurator.creator(m -> {
                        // return new RestClientBase(proxyType, baseUri).create();
                        ResultHandle interfaceHandle = m.loadClass(restClientName.toString());
                        ResultHandle baseUriHandle = m.load(getAnnotationParameter(entry.getValue(), "baseUri"));
                        ResultHandle configPrefixHandle = m.load(configPrefix);
                        ResultHandle baseHandle = m.newInstance(
                                MethodDescriptor.ofConstructor(RestClientBase.class, Class.class, String.class, String.class),
                                interfaceHandle, baseUriHandle, configPrefixHandle);
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
            } else if (capabilities.isCapabilityPresent(Capabilities.SERVLET)) {
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
}
