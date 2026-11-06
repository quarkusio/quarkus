package io.quarkus.amazon.lambda.deployment;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.inject.Named;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.joda.time.DateTime;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import io.quarkus.amazon.lambda.deployment.RequestHandlerJandexUtil.RequestHandlerJandexDefinition;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaRecorder;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaRecorder.RequestHandlerClassNames;
import io.quarkus.amazon.lambda.runtime.FunctionError;
import io.quarkus.amazon.lambda.runtime.LambdaBuildTimeConfig;
import io.quarkus.amazon.lambda.runtime.LambdaConfig;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.builder.BuildException;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.core.deployment.service.VoidServiceBuilder1;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.value.registry.ValueRegistry;

@SuppressWarnings("unchecked")
public final class AmazonLambdaProcessor {

    private static final String STATIC_HANDLER_SERVICE = "io.quarkus.amazon-lambda.static-handler";
    private static final String HANDLER_SERVICE = "io.quarkus.amazon-lambda.handler";
    public static final String AWS_LAMBDA_EVENTS_ARCHIVE_MARKERS = "com/amazonaws/services/lambda/runtime/events";

    private static final DotName REQUEST_HANDLER = DotName.createSimple(RequestHandler.class);
    private static final DotName REQUEST_STREAM_HANDLER = DotName.createSimple(RequestStreamHandler.class);
    private static final DotName SKILL_STREAM_HANDLER = DotName.createSimple("com.amazon.ask.SkillStreamHandler");

    private static final DotName NAMED = DotName.createSimple(Named.class.getName());
    private static final Logger log = Logger.getLogger(AmazonLambdaProcessor.class);

    private static final Predicate<ClassInfo> INCLUDE_HANDLER_PREDICATE = new Predicate<>() {

        @Override
        public boolean test(ClassInfo classInfo) {
            if (classInfo.isAbstract() || classInfo.hasAnnotation(DotNames.DECORATOR)) {
                return false;
            }

            return true;
        }
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.AMAZON_LAMBDA);
    }

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem marker() {
        return new AdditionalApplicationArchiveMarkerBuildItem(AWS_LAMBDA_EVENTS_ARCHIVE_MARKERS);
    }

    @BuildStep
    List<AmazonLambdaBuildItem> discover(CombinedIndexBuildItem combinedIndexBuildItem,
            Optional<ProvidedAmazonLambdaHandlerBuildItem> providedLambda,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer) throws BuildException {

        List<ClassInfo> requestHandlers = new ArrayList<>(
                combinedIndexBuildItem.getIndex().getAllKnownImplementations(REQUEST_HANDLER)
                        .stream().filter(INCLUDE_HANDLER_PREDICATE).toList());

        List<ClassInfo> streamHandlers = new ArrayList<>(combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(REQUEST_STREAM_HANDLER).stream().filter(INCLUDE_HANDLER_PREDICATE).toList());
        streamHandlers.addAll(combinedIndexBuildItem.getIndex()
                .getAllKnownSubclasses(SKILL_STREAM_HANDLER).stream().filter(INCLUDE_HANDLER_PREDICATE).toList());

        if ((!requestHandlers.isEmpty() || !streamHandlers.isEmpty()) && providedLambda.isPresent()) {
            throw new BuildException(
                    "Multiple handler classes.  You have a custom handler class and the " + providedLambda.get().getProvider()
                            + " extension.  Please remove one of them from your deployment.",
                    List.of());

        }
        AdditionalBeanBuildItem.Builder additionalBeansBuilder = AdditionalBeanBuildItem.builder().setUnremovable();
        List<AmazonLambdaBuildItem> amazonLambdas = new ArrayList<>();

        for (ClassInfo requestHandler : requestHandlers) {
            if (requestHandler.isAbstract()) {
                continue;
            }

            additionalBeansBuilder.addBeanClass(requestHandler.name().toString());
            amazonLambdas.add(new AmazonLambdaBuildItem(requestHandler.name().toString(), getCdiName(requestHandler), false));
        }

        for (ClassInfo streamHandler : streamHandlers) {
            if (streamHandler.isAbstract()) {
                continue;
            }

            additionalBeansBuilder.addBeanClass(streamHandler.name().toString());
            amazonLambdas.add(new AmazonLambdaBuildItem(streamHandler.name().toString(), getCdiName(streamHandler), true));
        }

        additionalBeanBuildItemBuildProducer.produce(additionalBeansBuilder.build());

        reflectiveClassBuildItemBuildProducer
                .produce(ReflectiveClassBuildItem.builder(FunctionError.class).methods().fields()
                        .reason(getClass().getName())
                        .build());

        return amazonLambdas;
    }

    @BuildStep
    void processProvidedLambda(Optional<ProvidedAmazonLambdaHandlerBuildItem> providedLambda,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer) {
        if (!providedLambda.isPresent())
            return;

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();
        Class handlerClass = providedLambda.get().getHandlerClass();
        builder.addBeanClass(handlerClass);
        additionalBeanBuildItemBuildProducer.produce(builder.build());

        reflectiveClassBuildItemBuildProducer
                .produce(ReflectiveClassBuildItem.builder(handlerClass).methods().fields()
                        .reason(getClass().getName())
                        .build());

        // TODO
        // This really isn't good enough.  We should recursively add reflection for all method and field types of the parameter
        // and return type.  Otherwise Jackson won't work.  In AWS Lambda HTTP extension, the whole jackson model is registered
        // for reflection.  Shouldn't have to do this.
        for (Method method : handlerClass.getMethods()) {
            if (method.getName().equals("handleRequest")
                    && method.getParameterCount() == 2) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (!parameterTypes[0].equals(Object.class)) {
                    reflectiveClassBuildItemBuildProducer
                            .produce(ReflectiveClassBuildItem.builder(parameterTypes[0].getName())
                                    .reason(getClass().getName() + " > " + method.getName() + " first parameter type")
                                    .methods().fields().build());
                    reflectiveClassBuildItemBuildProducer
                            .produce(ReflectiveClassBuildItem.builder(method.getReturnType().getName())
                                    .reason(getClass().getName() + " > " + method.getName() + " return type")
                                    .methods().fields().build());
                    reflectiveClassBuildItemBuildProducer
                            .produce(ReflectiveClassBuildItem.builder(DateTime.class)
                                    .reason(getClass().getName())
                                    .methods().fields().build());
                    break;
                }
            }
        }
    }

    @BuildStep
    public void recordStaticInitHandlerClass(CombinedIndexBuildItem index,
            List<AmazonLambdaBuildItem> lambdas,
            Optional<ProvidedAmazonLambdaHandlerBuildItem> providedLambda,
            ServiceRegistrar serviceRegistrar,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies) {
        // can set handler within static initialization if only one handler exists in deployment
        if (providedLambda.isPresent()) {
            boolean useStreamHandler = false;
            for (Class handleInterface : providedLambda.get().getHandlerClass().getInterfaces()) {
                if (handleInterface.getName().equals(RequestStreamHandler.class.getName())) {
                    useStreamHandler = true;
                    break;
                }
            }

            if (useStreamHandler) {
                registerStaticStreamHandler(serviceRegistrar, providedLambda.get().getHandlerClass().getName());
            } else {
                RequestHandlerJandexDefinition requestHandlerJandexDefinition = RequestHandlerJandexUtil
                        .discoverHandlerMethod(providedLambda.get().getHandlerClass().getName(), index.getComputingIndex());
                registerForReflection(requestHandlerJandexDefinition, reflectiveMethods, reflectiveHierarchies);
                registerStaticHandler(serviceRegistrar, toRequestHandlerClassNames(requestHandlerJandexDefinition));
            }
        } else if (lambdas != null && lambdas.size() == 1) {
            AmazonLambdaBuildItem item = lambdas.get(0);
            if (item.isStreamHandler()) {
                registerStaticStreamHandler(serviceRegistrar, item.getHandlerClass());
            } else {
                RequestHandlerJandexDefinition requestHandlerJandexDefinition = RequestHandlerJandexUtil
                        .discoverHandlerMethod(item.getHandlerClass(), index.getComputingIndex());
                registerForReflection(requestHandlerJandexDefinition, reflectiveMethods, reflectiveHierarchies);
                registerStaticHandler(serviceRegistrar, toRequestHandlerClassNames(requestHandlerJandexDefinition));
            }
        } else if (lambdas == null || lambdas.isEmpty()) {
            String errorMessage = "Unable to find handler class, make sure your deployment includes a single "
                    + RequestHandler.class.getName() + " or " + RequestStreamHandler.class.getName() + " implementation";
            throw new RuntimeException(errorMessage);
        }
    }

    private static void registerStaticStreamHandler(ServiceRegistrar serviceRegistrar, String handlerClass) {
        serviceRegistrar
                .forService(STATIC_HANDLER_SERVICE)
                .atPhase(Phase.STATIC_INIT)
                .onStart(ctx -> AmazonLambdaRecorder.setStreamHandlerClass(handlerClass));
    }

    private static void registerStaticHandler(ServiceRegistrar serviceRegistrar, RequestHandlerClassNames handler) {
        serviceRegistrar
                .forService(STATIC_HANDLER_SERVICE)
                .atPhase(Phase.STATIC_INIT)
                .afterBuildItem(LambdaObjectMapperInitializedBuildItem.class)
                .onStart(ctx -> AmazonLambdaRecorder.setHandlerClass(handler));
    }

    @BuildStep
    public void recordHandlerClass(CombinedIndexBuildItem index,
            List<AmazonLambdaBuildItem> lambdas,
            Optional<ProvidedAmazonLambdaHandlerBuildItem> providedLambda,
            ServiceRegistrar serviceRegistrar,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies) {
        // Have to set lambda class at runtime if there is not a provided lambda or there is more than one lambda in
        // deployment
        if (!providedLambda.isPresent() && lambdas != null && lambdas.size() > 1) {
            List<RequestHandlerClassNames> unnamed = new ArrayList<>();
            Map<String, RequestHandlerClassNames> named = new HashMap<>();

            List<String> unnamedStreamHandler = new ArrayList<>();
            Map<String, String> namedStreamHandler = new HashMap<>();

            for (AmazonLambdaBuildItem i : lambdas) {
                reflectiveClassBuildItemBuildProducer
                        .produce(ReflectiveClassBuildItem.builder(i.getHandlerClass()).constructors(false).build());
                if (i.isStreamHandler()) {
                    if (i.getName() == null) {
                        unnamedStreamHandler.add(i.getHandlerClass());
                    } else {
                        namedStreamHandler.put(i.getName(), i.getHandlerClass());
                    }
                } else {
                    if (i.getName() == null) {
                        RequestHandlerJandexDefinition requestHandlerJandexDefinition = RequestHandlerJandexUtil
                                .discoverHandlerMethod(i.getHandlerClass(), index.getComputingIndex());
                        registerForReflection(requestHandlerJandexDefinition, reflectiveMethods, reflectiveHierarchies);
                        unnamed.add(toRequestHandlerClassNames(requestHandlerJandexDefinition));
                    } else {
                        RequestHandlerJandexDefinition requestHandlerJandexDefinition = RequestHandlerJandexUtil
                                .discoverHandlerMethod(i.getHandlerClass(), index.getComputingIndex());
                        registerForReflection(requestHandlerJandexDefinition, reflectiveMethods, reflectiveHierarchies);
                        named.put(i.getName(), toRequestHandlerClassNames(requestHandlerJandexDefinition));
                    }
                }
            }

            List<RequestHandlerClassNames> capturedUnnamed = List.copyOf(unnamed);
            Map<String, RequestHandlerClassNames> capturedNamed = Map.copyOf(named);
            List<String> capturedUnnamedStreamHandler = List.copyOf(unnamedStreamHandler);
            Map<String, String> capturedNamedStreamHandler = Map.copyOf(namedStreamHandler);
            serviceRegistrar
                    .forService(HANDLER_SERVICE)
                    .atPhase(Phase.INIT)
                    .require(BeanContainer.class)
                    .require(LambdaConfig.class)
                    .onStart((ctx, beanContainer, config) -> {
                        AmazonLambdaRecorder.setBeanContainer(beanContainer);
                        AmazonLambdaRecorder.chooseHandlerClass(config, capturedUnnamed, capturedNamed,
                                capturedUnnamedStreamHandler, capturedNamedStreamHandler);
                    });
        } else {
            serviceRegistrar
                    .forService(HANDLER_SERVICE)
                    .atPhase(Phase.INIT)
                    .require(BeanContainer.class)
                    .onStart((ctx, beanContainer) -> AmazonLambdaRecorder.setBeanContainer(beanContainer));
        }
    }

    /**
     * This should only run when building a native image
     */
    @BuildStep(onlyIf = NativeBuild.class)
    void startPoolLoop(ServiceRegistrar serviceRegistrar,
            LaunchModeBuildItem launchModeBuildItem) {
        LaunchMode launchMode = launchModeBuildItem.getLaunchMode();
        pollLoopService(serviceRegistrar)
                .onStart((ctx, container) -> AmazonLambdaRecorder.startPollLoop(
                        container.instance(ValueRegistry.class).get(), ctx, launchMode));
    }

    @BuildStep(onlyIfNot = NativeBuild.class)
    void startPoolLoopDevOrTest(ServiceRegistrar serviceRegistrar,
            LaunchModeBuildItem launchModeBuildItem) {
        LaunchMode launchMode = launchModeBuildItem.getLaunchMode();
        pollLoopService(serviceRegistrar)
                .onStart((ctx, container) -> AmazonLambdaRecorder.startPollLoopIfDevOrTestOrExplicit(
                        container.instance(ValueRegistry.class).get(), ctx, launchMode));
    }

    private static VoidServiceBuilder1<ArcContainer> pollLoopService(ServiceRegistrar serviceRegistrar) {
        return serviceRegistrar
                .forService("io.quarkus.amazon-lambda.poll-loop")
                .atPhase(Phase.SERVING)
                .after(STATIC_HANDLER_SERVICE)
                .after(HANDLER_SERVICE)
                .require(ArcContainer.class)
                // the value registry is a runtime-init synthetic bean
                .afterBuildItem(SyntheticBeansRuntimeInitBuildItem.class)
                // start polling once the other services have started
                .afterBuildItem(ServiceStartBuildItem.class);
    }

    @BuildStep
    void recordExpectedExceptions(LambdaBuildTimeConfig config,
            BuildProducer<ReflectiveClassBuildItem> registerForReflection,
            ServiceRegistrar serviceRegistrar) {
        Set<Class<?>> classes = config.expectedExceptions().map(Set::copyOf).orElseGet(Set::of);
        classes.stream()
                .map(clazz -> ReflectiveClassBuildItem.builder(clazz).constructors(false)
                        .reason(getClass().getName() + " expectedExceptions")
                        .build())
                .forEach(registerForReflection::produce);
        serviceRegistrar
                .forService("io.quarkus.amazon-lambda.expected-exceptions")
                .atPhase(Phase.STATIC_INIT)
                .onStart(ctx -> AmazonLambdaRecorder.setExpectedExceptionClasses(classes));
    }

    private static String getCdiName(ClassInfo handler) {
        AnnotationInstance named = handler.declaredAnnotation(NAMED);
        if (named == null) {
            return null;
        }
        return named.value().asString();
    }

    private static void registerForReflection(RequestHandlerJandexDefinition requestHandlerJandexDefinition,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies) {
        String source = AmazonLambdaProcessor.class.getSimpleName() + " > "
                + requestHandlerJandexDefinition.method().declaringClass() + "[" + requestHandlerJandexDefinition.method()
                + "]";
        reflectiveHierarchies.produce(ReflectiveHierarchyBuildItem
                .builder(requestHandlerJandexDefinition.inputOutputTypes().inputType())
                .source(source)
                .build());
        reflectiveHierarchies.produce(ReflectiveHierarchyBuildItem
                .builder(requestHandlerJandexDefinition.inputOutputTypes().outputType())
                .source(source)
                .build());
        if (requestHandlerJandexDefinition.inputOutputTypes().isCollection()) {
            reflectiveMethods.produce(new ReflectiveMethodBuildItem(
                    "method reflectively accessed in io.quarkus.amazon.lambda.runtime.AmazonLambdaRecorder.discoverHandlerMethod",
                    requestHandlerJandexDefinition.method()));
            reflectiveHierarchies.produce(ReflectiveHierarchyBuildItem
                    .builder(requestHandlerJandexDefinition.inputOutputTypes().elementType())
                    .source(source)
                    .build());
        }
    }

    private static RequestHandlerClassNames toRequestHandlerClassNames(RequestHandlerJandexDefinition jandexDefinition) {
        return new RequestHandlerClassNames(
                jandexDefinition.handlerClass().name().toString(),
                jandexDefinition.method().declaringClass().name().toString(),
                jandexDefinition.inputOutputTypes().inputType().name().toString(),
                jandexDefinition.inputOutputTypes().outputType().name().toString());
    }
}
