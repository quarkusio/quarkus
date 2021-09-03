package io.quarkus.grpc.deployment;

import static io.quarkus.deployment.Feature.GRPC_SERVER;
import static java.util.Arrays.asList;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.grpc.internal.ServerImpl;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchivePredicateBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.deployment.devmode.FieldDefinalizingVisitor;
import io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator;
import io.quarkus.grpc.runtime.GrpcContainer;
import io.quarkus.grpc.runtime.GrpcServerRecorder;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerBuildTimeConfig;
import io.quarkus.grpc.runtime.health.GrpcHealthEndpoint;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.grpc.runtime.supports.context.GrpcRequestContextGrpcInterceptor;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.netty.deployment.MinNettyAllocatorMaxOrderBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;

public class GrpcServerProcessor {

    private static final Set<String> BLOCKING_SKIPPED_METHODS = Set.of("bindService", "<init>", "withCompression");

    private static final Logger logger = Logger.getLogger(GrpcServerProcessor.class);

    private static final String SSL_PREFIX = "quarkus.grpc.server.ssl.";
    private static final String CERTIFICATE = SSL_PREFIX + "certificate";
    private static final String KEY = SSL_PREFIX + "key";
    private static final String KEY_STORE = SSL_PREFIX + "key-store";
    private static final String TRUST_STORE = SSL_PREFIX + "trust-store";

    @BuildStep
    MinNettyAllocatorMaxOrderBuildItem setMinimalNettyMaxOrderSize() {
        return new MinNettyAllocatorMaxOrderBuildItem(3);
    }

    @BuildStep
    void processGeneratedBeans(CombinedIndexBuildItem index, BuildProducer<AnnotationsTransformerBuildItem> transformers,
            BuildProducer<BindableServiceBuildItem> bindables) {

        // generated bean class -> blocking methods
        Map<DotName, Set<MethodInfo>> generatedBeans = new HashMap<>();
        String[] excludedPackages = { "grpc.health.v1", "io.grpc.reflection" };

        // We need to transform the generated bean and register a bindable service if:
        // 1. there is a user-defined bean that implements the generated interface (injected delegate)
        // 2. there is no user-defined bean that extends the relevant impl bases (both mutiny and regular)
        for (ClassInfo generatedBean : index.getIndex().getKnownDirectImplementors(GrpcDotNames.MUTINY_BEAN)) {
            FieldInfo delegateField = generatedBean.field("delegate");
            if (delegateField == null) {
                throw new IllegalStateException("A generated bean does not declare the delegate field: " + generatedBean);
            }
            DotName serviceInterface = delegateField.type().name();
            Collection<ClassInfo> serviceCandidates = index.getIndex().getAllKnownImplementors(serviceInterface);
            if (serviceCandidates.isEmpty()) {
                // No user-defined bean that implements the generated interface
                continue;
            }
            ClassInfo userDefinedBean = null;
            for (ClassInfo candidate : serviceCandidates) {
                // The bean must be annotated with @GrpcService
                if (candidate.classAnnotation(GrpcDotNames.GRPC_SERVICE) != null) {
                    userDefinedBean = candidate;
                    break;
                }
            }
            if (userDefinedBean == null) {
                continue;
            }
            DotName mutinyImplBase = generatedBean.superName();
            if (index.getIndex().getAllKnownSubclasses(mutinyImplBase).size() != 1) {
                // Some class extends the mutiny impl base
                continue;
            }
            String mutinyImplBaseName = mutinyImplBase.toString();
            // Now derive the original impl base
            // e.g. examples.MutinyGreeterGrpc.GreeterImplBase -> examples.GreeterGrpc.GreeterImplBase
            DotName implBase = DotName.createSimple(mutinyImplBaseName.replace(MutinyGrpcGenerator.CLASS_PREFIX, ""));
            if (!index.getIndex().getAllKnownSubclasses(implBase).isEmpty()) {
                // Some class extends the impl base
                continue;
            }
            // Finally exclude some packages
            boolean excluded = false;
            for (String excludedPackage : excludedPackages) {
                if (mutinyImplBaseName.startsWith(excludedPackage)) {
                    excluded = true;
                    break;
                }
            }
            if (!excluded) {
                logger.debugf("Registering generated gRPC bean %s that will delegate to %s", generatedBean, userDefinedBean);
                Set<MethodInfo> blockingMethods = gatherBlockingMethods(userDefinedBean);

                generatedBeans.put(generatedBean.name(), blockingMethods);
            }
        }

        if (!generatedBeans.isEmpty()) {
            // For every suitable bean we must:
            // (a) add @Singleton and @GrpcService
            // (b) register a BindableServiceBuildItem, incl. all blocking methods (derived from the user-defined impl)
            for (Entry<DotName, Set<MethodInfo>> entry : generatedBeans.entrySet()) {
                BindableServiceBuildItem bindableService = new BindableServiceBuildItem(entry.getKey());
                for (MethodInfo blockingMethod : entry.getValue()) {
                    bindableService.registerBlockingMethod(blockingMethod.name());
                }
                bindables.produce(bindableService);
            }
            transformers.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
                @Override
                public boolean appliesTo(Kind kind) {
                    return kind == Kind.CLASS;
                }

                @Override
                public void transform(TransformationContext context) {
                    if (generatedBeans.containsKey(context.getTarget().asClass().name())) {
                        context.transform()
                                .add(BuiltinScope.SINGLETON.getName())
                                .add(GrpcDotNames.GRPC_SERVICE)
                                .done();
                    }
                }
            }));
        }
    }

    @BuildStep
    void discoverBindableServices(BuildProducer<BindableServiceBuildItem> bindables,
            CombinedIndexBuildItem combinedIndexBuildItem) {
        Collection<ClassInfo> bindableServices = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(GrpcDotNames.BINDABLE_SERVICE);

        for (ClassInfo service : bindableServices) {
            if (service.interfaceNames().contains(GrpcDotNames.MUTINY_BEAN)) {
                // Ignore the generated beans
                continue;
            }
            if (Modifier.isAbstract(service.flags())) {
                continue;
            }
            BindableServiceBuildItem item = new BindableServiceBuildItem(service.name());
            Set<MethodInfo> blockingMethods = gatherBlockingMethods(service);
            for (MethodInfo method : blockingMethods) {
                item.registerBlockingMethod(method.name());
            }
            bindables.produce(item);
        }
    }

    private Set<MethodInfo> gatherBlockingMethods(ClassInfo service) {
        Set<MethodInfo> result = new HashSet<>();
        boolean classHasBlocking = service.classAnnotation(GrpcDotNames.BLOCKING) != null;
        for (MethodInfo method : service.methods()) {
            if (BLOCKING_SKIPPED_METHODS.contains(method.name())) {
                continue;
            }
            if (method.hasAnnotation(GrpcDotNames.BLOCKING)
                    || (classHasBlocking && !method.hasAnnotation(GrpcDotNames.NON_BLOCKING))) {
                result.add(method);
            }
        }
        return result;
    }

    @BuildStep
    AnnotationsTransformerBuildItem transformUserDefinedServices(CombinedIndexBuildItem combinedIndexBuildItem,
            CustomScopeAnnotationsBuildItem customScopes) {
        // User-defined services usually only declare the @GrpcService qualifier
        // We need to add @GrpcEnableRequestContext and @Singleton if needed
        Set<DotName> userDefinedServices = new HashSet<>();
        for (AnnotationInstance annotation : combinedIndexBuildItem.getIndex().getAnnotations(GrpcDotNames.GRPC_SERVICE)) {
            if (annotation.target().kind() == Kind.CLASS) {
                userDefinedServices.add(annotation.target().asClass().name());
            }
        }
        if (userDefinedServices.isEmpty()) {
            return null;
        }
        return new AnnotationsTransformerBuildItem(
                new AnnotationsTransformer() {
                    @Override
                    public boolean appliesTo(Kind kind) {
                        return kind == Kind.CLASS;
                    }

                    @Override
                    public void transform(TransformationContext context) {
                        ClassInfo clazz = context.getTarget().asClass();
                        if (userDefinedServices.contains(clazz.name()) && !customScopes.isScopeDeclaredOn(clazz)) {
                            // Add @Singleton to make it a bean
                            context.transform()
                                    .add(BuiltinScope.SINGLETON.getName())
                                    .done();
                        }
                    }
                });
    }

    @BuildStep
    void validateBindableServices(ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errors) {
        Type mutinyBeanType = Type.create(GrpcDotNames.MUTINY_BEAN, org.jboss.jandex.Type.Kind.CLASS);
        Type mutinyServiceType = Type.create(GrpcDotNames.MUTINY_SERVICE, org.jboss.jandex.Type.Kind.CLASS);
        Type bindableServiceType = Type.create(GrpcDotNames.BINDABLE_SERVICE, org.jboss.jandex.Type.Kind.CLASS);
        Predicate<Set<Type>> predicate = new Predicate<>() {
            @Override
            public boolean test(Set<Type> types) {
                return types.contains(bindableServiceType) || types.contains(mutinyServiceType);
            }
        };
        for (BeanInfo bean : validationPhase.getContext().beans().classBeans().matchBeanTypes(predicate)) {
            validateBindableService(bean, mutinyBeanType, errors);
        }
        // Validate the removed beans as well - detect beans not annotated with @GrpcService
        for (BeanInfo bean : validationPhase.getContext().removedBeans().classBeans().matchBeanTypes(predicate)) {
            validateBindableService(bean, mutinyBeanType, errors);
        }
    }

    private void validateBindableService(BeanInfo bean, Type generatedBeanType,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errors) {
        if (!bean.getTypes().contains(generatedBeanType) && bean.getQualifiers().stream().map(AnnotationInstance::name)
                .noneMatch(GrpcDotNames.GRPC_SERVICE::equals)) {
            errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                    new IllegalStateException(
                            "A gRPC service bean must be annotated with @io.quarkus.grpc.GrpcService: " + bean)));
        }
        if (!bean.getScope().getDotName().equals(BuiltinScope.SINGLETON.getName())) {
            errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                    new IllegalStateException("A gRPC service bean must have the javax.inject.Singleton scope: " + bean)));
        }
    }

    @BuildStep(onlyIf = IsNormal.class)
    KubernetesPortBuildItem registerGrpcServiceInKubernetes(List<BindableServiceBuildItem> bindables) {
        if (!bindables.isEmpty()) {
            int port = ConfigProvider.getConfig().getOptionalValue("quarkus.grpc.server.port", Integer.class)
                    .orElse(9000);
            return new KubernetesPortBuildItem(port, GRPC_SERVER);
        }
        return null;
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> beans,
            List<BindableServiceBuildItem> bindables, BuildProducer<FeatureBuildItem> features) {
        // @GrpcService is a CDI qualifier
        beans.produce(new AdditionalBeanBuildItem(GrpcService.class));

        if (!bindables.isEmpty() || LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcContainer.class));

            // this makes GrpcRequestContextGrpcInterceptor registered as a global gRPC interceptor.
            // Global interceptors are invoked before any of the per-service interceptors
            beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcRequestContextGrpcInterceptor.class));
            features.produce(new FeatureBuildItem(GRPC_SERVER));
        } else {
            logger.debug("Unable to find beans exposing the `BindableService` interface - not starting the gRPC server");
        }
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    ServiceStartBuildItem initializeServer(GrpcServerRecorder recorder, GrpcConfiguration config,
            ShutdownContextBuildItem shutdown, List<BindableServiceBuildItem> bindables,
            LaunchModeBuildItem launchModeBuildItem,
            VertxBuildItem vertx) {

        // Build the list of blocking methods per service implementation
        Map<String, List<String>> blocking = new HashMap<>();
        for (BindableServiceBuildItem bindable : bindables) {
            if (bindable.hasBlockingMethods()) {
                blocking.put(bindable.serviceClass.toString(), bindable.blockingMethods);
            }
        }

        if (!bindables.isEmpty() || LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            recorder.initializeGrpcServer(vertx.getVertx(), config, shutdown, blocking, launchModeBuildItem.getLaunchMode());
            return new ServiceStartBuildItem(GRPC_SERVER);
        }
        return null;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void definializeGrpcFieldsForDevMode(BuildProducer<BytecodeTransformerBuildItem> transformers) {
        transformers.produce(new BytecodeTransformerBuildItem("io.grpc.internal.InternalHandlerRegistry",
                new FieldDefinalizingVisitor("services", "methods")));
        transformers.produce(new BytecodeTransformerBuildItem(ServerImpl.class.getName(),
                new FieldDefinalizingVisitor("interceptors")));
    }

    @BuildStep
    void addHealthChecks(GrpcServerBuildTimeConfig config,
            List<BindableServiceBuildItem> bindables,
            BuildProducer<HealthBuildItem> healthBuildItems,
            BuildProducer<AdditionalBeanBuildItem> beans) {
        boolean healthEnabled = false;
        if (!bindables.isEmpty()) {
            healthEnabled = config.mpHealthEnabled;

            if (config.grpcHealthEnabled) {
                beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcHealthEndpoint.class));
                healthEnabled = true;
            }
            healthBuildItems.produce(new HealthBuildItem("io.quarkus.grpc.runtime.health.GrpcHealthCheck",
                    config.mpHealthEnabled));
        }
        if (healthEnabled || LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcHealthStorage.class));
        }
    }

    @BuildStep
    void registerSslResources(BuildProducer<NativeImageResourceBuildItem> resourceBuildItem) {
        Config config = ConfigProvider.getConfig();
        for (String sslProperty : asList(CERTIFICATE, KEY, KEY_STORE, TRUST_STORE)) {
            config.getOptionalValue(sslProperty, String.class)
                    .ifPresent(value -> ResourceRegistrationUtils.registerResourceForProperty(resourceBuildItem, value));
        }
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem extensionSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(GRPC_SERVER);
    }

    @BuildStep
    void configureMetrics(GrpcBuildTimeConfig configuration, Optional<MetricsCapabilityBuildItem> metricsCapability,
            BuildProducer<AdditionalBeanBuildItem> beans) {

        // Note that this build steps confgures both the server side and the client side
        if (configuration.metricsEnabled && metricsCapability.isPresent()) {
            if (metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)) {
                // Strings are used intentionally - micrometer-core is an optional dependency of the runtime module
                beans.produce(new AdditionalBeanBuildItem("io.quarkus.grpc.runtime.metrics.GrpcMetricsServerInterceptor",
                        "io.quarkus.grpc.runtime.metrics.GrpcMetricsClientInterceptor"));
            } else {
                logger.warn("Only Micrometer-based metrics system is supported by quarkus-grpc");
            }
        }
    }

    @BuildStep
    BeanArchivePredicateBuildItem additionalBeanArchives() {
        return new BeanArchivePredicateBuildItem(new Predicate<ApplicationArchive>() {

            @Override
            public boolean test(ApplicationArchive archive) {
                // Every archive that contains a generated implementor of MutinyBean is considered a bean archive
                return !archive.getIndex().getKnownDirectImplementors(GrpcDotNames.MUTINY_BEAN).isEmpty();
            }
        });
    }

}
