package io.quarkus.grpc.deployment;

import static io.quarkus.deployment.Feature.GRPC_SERVER;
import static io.quarkus.grpc.deployment.GrpcDotNames.*;
import static io.quarkus.grpc.deployment.GrpcInterceptors.MICROMETER_INTERCEPTORS;
import static java.util.Arrays.asList;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.DeploymentException;
import javax.transaction.Transaction;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.grpc.internal.ServerImpl;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanArchivePredicateBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.deployment.RecorderBeanInitializedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
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
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.auth.DefaultAuthExceptionHandlerProvider;
import io.quarkus.grpc.auth.GrpcSecurityInterceptor;
import io.quarkus.grpc.deployment.devmode.FieldDefinalizingVisitor;
import io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator;
import io.quarkus.grpc.runtime.GrpcContainer;
import io.quarkus.grpc.runtime.GrpcServerRecorder;
import io.quarkus.grpc.runtime.ServerInterceptorStorage;
import io.quarkus.grpc.runtime.config.GrpcClientBuildTimeConfig;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerBuildTimeConfig;
import io.quarkus.grpc.runtime.health.GrpcHealthEndpoint;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.grpc.runtime.stork.GrpcStorkRecorder;
import io.quarkus.grpc.runtime.supports.context.GrpcDuplicatedContextGrpcInterceptor;
import io.quarkus.grpc.runtime.supports.context.GrpcRequestContextGrpcInterceptor;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.netty.deployment.MinNettyAllocatorMaxOrderBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;

public class GrpcServerProcessor {

    private static final Set<String> BLOCKING_SKIPPED_METHODS = Set.of("bindService", "<init>", "<clinit>", "withCompression");

    private static final Logger log = Logger.getLogger(GrpcServerProcessor.class);

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
            BuildProducer<BindableServiceBuildItem> bindables,
            BuildProducer<DelegatingGrpcBeanBuildItem> delegatingBeans) {

        // generated bean class -> blocking methods
        Map<DotName, Set<String>> generatedBeans = new HashMap<>();
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
            // Finally, exclude some packages
            boolean excluded = false;
            for (String excludedPackage : excludedPackages) {
                if (mutinyImplBaseName.startsWith(excludedPackage)) {
                    excluded = true;
                    break;
                }
            }
            if (!excluded) {
                log.debugf("Registering generated gRPC bean %s that will delegate to %s", generatedBean, userDefinedBean);
                delegatingBeans.produce(new DelegatingGrpcBeanBuildItem(generatedBean, userDefinedBean));
                Set<String> blockingMethods = gatherBlockingMethodNames(userDefinedBean, index.getIndex());

                generatedBeans.put(generatedBean.name(), blockingMethods);
            }
        }

        if (!generatedBeans.isEmpty()) {
            // For every suitable bean we must:
            // (a) add @Singleton and @GrpcService
            // (b) register a BindableServiceBuildItem, incl. all blocking methods (derived from the user-defined impl)
            for (Entry<DotName, Set<String>> entry : generatedBeans.entrySet()) {
                BindableServiceBuildItem bindableService = new BindableServiceBuildItem(entry.getKey());
                for (String blockingMethod : entry.getValue()) {
                    bindableService.registerBlockingMethod(blockingMethod);
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
        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<ClassInfo> bindableServices = index.getAllKnownImplementors(GrpcDotNames.BINDABLE_SERVICE);

        for (ClassInfo service : bindableServices) {
            if (service.interfaceNames().contains(GrpcDotNames.MUTINY_BEAN)) {
                // Ignore the generated beans
                continue;
            }
            if (Modifier.isAbstract(service.flags())) {
                continue;
            }
            BindableServiceBuildItem item = new BindableServiceBuildItem(service.name());
            Set<String> blockingMethods = gatherBlockingMethodNames(service, index);
            for (String method : blockingMethods) {
                item.registerBlockingMethod(method);
            }
            bindables.produce(item);
        }
    }

    /**
     * Generate list of {@link ClassInfo} with {@code service} as the first element and the class implementing
     * {@code io.grpc.BindableService} (for example via the protobuf generated {@code *ImplBase}) as the last one.
     * Implementing {@code BindableService} is not mandatory.
     */
    private static List<ClassInfo> classHierarchy(ClassInfo service, IndexView index) {
        List<ClassInfo> collected = new ArrayList<>();
        while (service != null) {
            collected.add(service);

            // Stop at the class that implements io.grpc.BindableService (implementing BindableService is not mandatory)
            if (service.interfaceNames().contains(GrpcDotNames.BINDABLE_SERVICE)) {
                break;
            }

            DotName superName = service.superName();
            if (superName == null) {
                break;
            }
            service = index.getClassByName(superName);
        }
        return collected;
    }

    private enum BlockingMode {
        UNDEFINED(false),
        BLOCKING(true),
        NON_BLOCKING(false),
        // @Transactional on a method
        IMPLICIT(true);

        final boolean blocking;

        BlockingMode(boolean blocking) {
            this.blocking = blocking;
        }
    }

    private static BlockingMode nonInheritedBlockingMode(Predicate<DotName> checker,
            Supplier<String> exceptionMsgSupplier) {
        boolean blocking = checker.test(BLOCKING);
        boolean nonBlocking = checker.test(NON_BLOCKING);
        if (blocking && nonBlocking) {
            throw new DeploymentException(exceptionMsgSupplier.get());
        }
        if (blocking) {
            return BlockingMode.BLOCKING;
        }
        if (nonBlocking) {
            return BlockingMode.NON_BLOCKING;
        }
        boolean transactional = checker.test(TRANSACTIONAL);
        if (transactional) {
            return BlockingMode.IMPLICIT;
        }
        return BlockingMode.UNDEFINED;
    }

    /**
     * Retrieve the blocking mode determined by inheritable annotations declared on a class or method.
     *
     * @param currentlyKnownMode currently known mode.
     */
    private static BlockingMode inheritedBlockingMode(Predicate<DotName> checker,
            BlockingMode currentlyKnownMode) {
        boolean transactional = checker.test(TRANSACTIONAL);
        if (transactional) {
            return BlockingMode.IMPLICIT;
        }
        return currentlyKnownMode;
    }

    /**
     * Retrieve the blocking-mode for the given method name.
     *
     * <p>
     * Traverses the service impl class hierarchy, stops at the first "explict" annotation
     * ({@link io.smallrye.common.annotation.Blocking} or {@link io.smallrye.common.annotation.NonBlocking}).
     *
     * <p>
     * Otherwise returns the "topmost" "non-explicit" annotation (aka {@link javax.transaction.Transactional}).
     */
    private static boolean methodIsBlocking(List<ClassInfo> classes, String methodName, Type[] methodArgs) {
        BlockingMode classModeInherited = BlockingMode.UNDEFINED;
        BlockingMode methodMode = BlockingMode.UNDEFINED;
        for (int i = 0; i < classes.size(); i++) {
            ClassInfo ci = classes.get(i);
            Predicate<DotName> annotationOnClass = annotationName -> ci.classAnnotation(annotationName) != null;
            MethodInfo method = ci.method(methodName, methodArgs);

            classModeInherited = inheritedBlockingMode(annotationOnClass, classModeInherited);

            if (method != null) {
                Predicate<DotName> annotationOnMethod = n -> {
                    AnnotationInstance annotationInstance = method.annotation(n);
                    return annotationInstance != null && annotationInstance.target().kind() == Kind.METHOD;
                };
                methodMode = nonInheritedBlockingMode(annotationOnMethod,
                        () -> "Method '" + method.declaringClass().name() + "#" + method.name() +
                                "' contains both @Blocking and @NonBlocking annotations.");
                if (methodMode == BlockingMode.UNDEFINED) {
                    methodMode = nonInheritedBlockingMode(annotationOnClass,
                            () -> "Class '" + ci.name() + "' contains both @Blocking and @NonBlocking annotations.");
                }

                // Handles the case when a method's overridden without an explicit annotation and @Transactional is defined on a superclass
                if (methodMode == BlockingMode.UNDEFINED) {
                    for (i++; i < classes.size(); i++) {
                        ClassInfo ci2 = classes.get(i);
                        annotationOnClass = annotationName -> ci2.classAnnotation(annotationName) != null;
                        classModeInherited = inheritedBlockingMode(annotationOnClass, classModeInherited);
                    }
                }

                break;
            }
        }
        if (methodMode != BlockingMode.UNDEFINED) {
            return methodMode.blocking;
        }
        return classModeInherited.blocking;
    }

    /**
     * Collect the names of all blocking methods.
     *
     * <p>
     * Whether a method is blocking or not is evaluated for each individual service method (those that are defined
     * in the generated {@code *ImplBase} class).
     *
     * <p>
     * For each method:
     * <ol>
     * <li>blocking, if the top-most method override has a {@link io.smallrye.common.annotation.Blocking} annotation.</li>
     * <li>not-blocking, if the top-most method override has a {@link io.smallrye.common.annotation.NonBlocking}
     * annotation.</li>
     * <li>blocking, if the class that with the top-most method override has a {@link io.smallrye.common.annotation.Blocking}
     * annotation.</li>
     * <li>non-blocking, if the class that with the top-most method override has a
     * {@link io.smallrye.common.annotation.NonBlocking} annotation.</li>
     * <li>blocking, if top-most method override has a {@link Transaction} annotation.</li>
     * <li>blocking, if the service class or any of its base classes has a {@link Transaction}
     * annotation.</li>
     * <li>Else: non-blocking.</li>
     * </ol>
     */
    static Set<String> gatherBlockingMethodNames(ClassInfo service, IndexView index) {
        List<ClassInfo> classes = classHierarchy(service, index);

        Set<String> result = new HashSet<>();

        // Collect all gRPC methods from the *ImplBase class, if present
        List<MethodInfo> implBaseMethods = classes.get(classes.size() - 1).methods();

        for (MethodInfo implBaseMethod : implBaseMethods) {
            String methodName = implBaseMethod.name();
            if (BLOCKING_SKIPPED_METHODS.contains(methodName)) {
                continue;
            }

            // Find the annotations for the current method.
            if (methodIsBlocking(classes, methodName, implBaseMethod.parameters().toArray(new Type[0]))) {
                result.add(methodName);
            }
        }

        log.debugf("Blocking methods for class '%s': %s", service.name(), result);

        return result;
    }

    @BuildStep
    AnnotationsTransformerBuildItem transformUserDefinedServices(CombinedIndexBuildItem combinedIndexBuildItem,
            CustomScopeAnnotationsBuildItem customScopes) {
        // User-defined services usually only declare the @GrpcService qualifier
        // We need to add @Singleton if needed
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
            Capabilities capabilities,
            List<BindableServiceBuildItem> bindables, BuildProducer<FeatureBuildItem> features) {
        // @GrpcService is a CDI qualifier
        beans.produce(new AdditionalBeanBuildItem(GrpcService.class));

        if (!bindables.isEmpty() || LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcContainer.class));

            // this makes GrpcRequestContextGrpcInterceptor registered as a global gRPC interceptor.
            // Global interceptors are invoked before any of the per-service interceptors
            beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcRequestContextGrpcInterceptor.class));
            beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcDuplicatedContextGrpcInterceptor.class));
            features.produce(new FeatureBuildItem(GRPC_SERVER));

            if (capabilities.isPresent(Capability.SECURITY)) {
                beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcSecurityInterceptor.class));
                beans.produce(AdditionalBeanBuildItem.unremovableOf(DefaultAuthExceptionHandlerProvider.class));
            }
        } else {
            log.debug("Unable to find beans exposing the `BindableService` interface - not starting the gRPC server");
        }
    }

    @BuildStep
    void registerAdditionalInterceptors(BuildProducer<AdditionalGlobalInterceptorBuildItem> additionalInterceptors,
            Capabilities capabilities) {
        additionalInterceptors
                .produce(new AdditionalGlobalInterceptorBuildItem(GrpcRequestContextGrpcInterceptor.class.getName()));
        additionalInterceptors
                .produce(new AdditionalGlobalInterceptorBuildItem(GrpcRequestContextGrpcInterceptor.class.getName()));
        if (capabilities.isPresent(Capability.SECURITY)) {
            additionalInterceptors
                    .produce(new AdditionalGlobalInterceptorBuildItem(GrpcSecurityInterceptor.class.getName()));
        }
    }

    @SuppressWarnings("deprecation")
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void gatherGrpcInterceptors(BeanArchiveIndexBuildItem indexBuildItem,
            List<AdditionalGlobalInterceptorBuildItem> additionalGlobalInterceptors,
            List<DelegatingGrpcBeanBuildItem> delegatingGrpcBeans,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            RecorderContext recorderContext,
            GrpcServerRecorder recorder) {

        Map<String, String> delegateMap = new HashMap<>();
        for (DelegatingGrpcBeanBuildItem delegatingGrpcBean : delegatingGrpcBeans) {
            delegateMap.put(delegatingGrpcBean.userDefinedBean.name().toString(),
                    delegatingGrpcBean.generatedBean.name().toString());
        }

        IndexView index = indexBuildItem.getIndex();

        GrpcInterceptors interceptors = GrpcInterceptors.gatherInterceptors(index, GrpcDotNames.SERVER_INTERCEPTOR);

        // let's gather all the non-abstract, non-global interceptors, from these we'll filter out ones used per-service ones
        // the rest, if anything stays, should be logged as problematic
        Set<String> superfluousInterceptors = new HashSet<>(interceptors.nonGlobalInterceptors);

        // Remove the metrics interceptors
        for (String MICROMETER_INTERCEPTOR : MICROMETER_INTERCEPTORS) {
            superfluousInterceptors.remove(MICROMETER_INTERCEPTOR);
        }

        List<AnnotationInstance> found = new ArrayList<>(index.getAnnotations(GrpcDotNames.REGISTER_INTERCEPTOR));
        for (AnnotationInstance annotation : index.getAnnotations(GrpcDotNames.REGISTER_INTERCEPTORS)) {
            for (AnnotationInstance nested : annotation.value().asNestedArray()) {
                found.add(AnnotationInstance.create(nested.name(), annotation.target(), nested.values()));
            }
        }

        Map<String, Set<String>> registeredInterceptors = new HashMap<>();
        for (AnnotationInstance annotation : found) {
            String interceptorClass = annotation.value().asString();
            if (annotation.target().kind() != Kind.CLASS) {
                throw new IllegalStateException("Invalid target for the @RegisterInterceptor: " + annotation.target());
            }
            String targetClass = annotation.target().asClass().name().toString();

            // if the user bean is invoked by a generated bean
            // the interceptors defined on the user bean have to be applied to the generated bean:
            targetClass = delegateMap.getOrDefault(targetClass, targetClass);

            Set<String> registered = registeredInterceptors.get(targetClass);
            if (registered == null) {
                registered = new HashSet<>();
                registeredInterceptors.put(targetClass, registered);
            }
            registered.add(interceptorClass);
            superfluousInterceptors.remove(interceptorClass);
        }

        Set<Class<?>> globalInterceptors = new HashSet<>();
        for (String interceptor : interceptors.globalInterceptors) {
            globalInterceptors.add(recorderContext.classProxy(interceptor));
        }
        for (AdditionalGlobalInterceptorBuildItem globalInterceptorBuildItem : additionalGlobalInterceptors) {
            globalInterceptors.add(recorderContext.classProxy(globalInterceptorBuildItem.interceptorClass()));
        }

        Map<String, Set<Class<?>>> perClientInterceptors = new HashMap<>();
        for (Entry<String, Set<String>> entry : registeredInterceptors.entrySet()) {
            Set<Class<?>> interceptorClasses = new HashSet<>();
            for (String interceptorClass : entry.getValue()) {
                interceptorClasses.add(recorderContext.classProxy(interceptorClass));
            }
            perClientInterceptors.put(entry.getKey(), interceptorClasses);
        }

        syntheticBeans.produce(
                SyntheticBeanBuildItem.configure(ServerInterceptorStorage.class)
                        .unremovable()
                        .runtimeValue(recorder.initServerInterceptorStorage(perClientInterceptors, globalInterceptors))
                        .setRuntimeInit()
                        .done());

        if (!superfluousInterceptors.isEmpty()) {
            log.warnf("At least one unused gRPC interceptor found: %s. If there are meant to be used globally, " +
                    "annotate them with @GlobalInterceptor.", String.join(", ", superfluousInterceptors));
        }
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    ServiceStartBuildItem initializeServer(GrpcServerRecorder recorder,
            GrpcConfiguration config,
            GrpcBuildTimeConfig buildTimeConfig,
            ShutdownContextBuildItem shutdown,
            List<BindableServiceBuildItem> bindables,
            List<RecorderBeanInitializedBuildItem> orderEnforcer,
            LaunchModeBuildItem launchModeBuildItem,
            VertxBuildItem vertx) {

        // Build the list of blocking methods per service implementation
        Map<String, List<String>> blocking = new HashMap<>();
        for (BindableServiceBuildItem bindable : bindables) {
            if (bindable.hasBlockingMethods()) {
                blocking.put(bindable.serviceClass.toString(), bindable.blockingMethods);
            }
        }

        if (!bindables.isEmpty()
                || (LaunchMode.current() == LaunchMode.DEVELOPMENT && buildTimeConfig.devMode.forceServerStart)) {
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
    @Record(ExecutionTime.STATIC_INIT)
    void setUpStork(GrpcStorkRecorder storkRecorder, GrpcClientBuildTimeConfig config) {
        storkRecorder.init(config.storkProactiveConnections);
    }

    @BuildStep
    BeanArchivePredicateBuildItem additionalBeanArchives() {
        return new BeanArchivePredicateBuildItem(new Predicate<>() {

            @Override
            public boolean test(ApplicationArchive archive) {
                // Every archive that contains a generated implementor of MutinyBean is considered a bean archive
                return !archive.getIndex().getKnownDirectImplementors(GrpcDotNames.MUTINY_BEAN).isEmpty();
            }
        });
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableServerInterceptors() {
        return UnremovableBeanBuildItem.beanTypes(GrpcDotNames.SERVER_INTERCEPTOR);
    }

}
