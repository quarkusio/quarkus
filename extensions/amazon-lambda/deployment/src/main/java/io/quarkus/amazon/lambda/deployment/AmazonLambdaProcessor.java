package io.quarkus.amazon.lambda.deployment;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Named;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;
import org.joda.time.DateTime;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaRecorder;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaStaticRecorder;
import io.quarkus.amazon.lambda.runtime.FunctionError;
import io.quarkus.amazon.lambda.runtime.LambdaBuildTimeConfig;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.LaunchMode;

@SuppressWarnings("unchecked")
public final class AmazonLambdaProcessor {
    public static final String AWS_LAMBDA_EVENTS_ARCHIVE_MARKERS = "com/amazonaws/services/lambda/runtime/events";

    private static final DotName REQUEST_HANDLER = DotName.createSimple(RequestHandler.class.getName());
    private static final DotName REQUEST_STREAM_HANDLER = DotName.createSimple(RequestStreamHandler.class.getName());
    private static final DotName SKILL_STREAM_HANDLER = DotName.createSimple("com.amazon.ask.SkillStreamHandler");

    private static final DotName NAMED = DotName.createSimple(Named.class.getName());
    private static final Logger log = Logger.getLogger(AmazonLambdaProcessor.class);

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

        Collection<ClassInfo> allKnownImplementors = combinedIndexBuildItem.getIndex().getAllKnownImplementors(REQUEST_HANDLER);
        allKnownImplementors.addAll(combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(REQUEST_STREAM_HANDLER));
        allKnownImplementors.addAll(combinedIndexBuildItem.getIndex()
                .getAllKnownSubclasses(SKILL_STREAM_HANDLER));

        if (allKnownImplementors.size() > 0 && providedLambda.isPresent()) {
            throw new BuildException(
                    "Multiple handler classes.  You have a custom handler class and the " + providedLambda.get().getProvider()
                            + " extension.  Please remove one of them from your deployment.",
                    Collections.emptyList());

        }
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();
        List<AmazonLambdaBuildItem> ret = new ArrayList<>();

        for (ClassInfo info : allKnownImplementors) {
            if (Modifier.isAbstract(info.flags())) {
                continue;
            }

            final DotName name = info.name();
            final String lambda = name.toString();
            builder.addBeanClass(lambda);
            reflectiveClassBuildItemBuildProducer
                    .produce(ReflectiveClassBuildItem.builder(lambda).methods().build());

            String cdiName = null;
            AnnotationInstance named = info.declaredAnnotation(NAMED);
            if (named != null) {
                cdiName = named.value().asString();
            }

            ClassInfo current = info;
            boolean done = false;
            boolean streamHandler = info.superName().equals(SKILL_STREAM_HANDLER);
            while (current != null && !done) {
                for (MethodInfo method : current.methods()) {
                    if (method.name().equals("handleRequest")) {
                        if (method.parametersCount() == 3) {
                            streamHandler = true;
                            done = true;
                            break;
                        } else if (method.parametersCount() == 2
                                && !method.parameterType(0).name().equals(DotName.createSimple(Object.class.getName()))) {
                            String source = getClass().getSimpleName() + " > " + method.declaringClass() + "[" + method + "]";

                            reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                                    .type(method.parameterType(0))
                                    .source(source)
                                    .build());
                            reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                                    .type(method.returnType())
                                    .source(source)
                                    .build());
                            done = true;
                            break;
                        }
                    }
                }
                current = combinedIndexBuildItem.getIndex().getClassByName(current.superName());
            }
            ret.add(new AmazonLambdaBuildItem(lambda, cdiName, streamHandler));
        }
        additionalBeanBuildItemBuildProducer.produce(builder.build());
        reflectiveClassBuildItemBuildProducer
                .produce(ReflectiveClassBuildItem.builder(FunctionError.class).methods().fields()
                        .build());
        return ret;
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
                .produce(ReflectiveClassBuildItem.builder(handlerClass).methods().fields().build());

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
                                    .methods().fields().build());
                    reflectiveClassBuildItemBuildProducer
                            .produce(ReflectiveClassBuildItem.builder(method.getReturnType().getName())
                                    .methods().fields().build());
                    reflectiveClassBuildItemBuildProducer.produce(ReflectiveClassBuildItem.builder(DateTime.class)
                            .methods().fields().build());
                    break;
                }
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void recordStaticInitHandlerClass(List<AmazonLambdaBuildItem> lambdas,
            LambdaObjectMapperInitializedBuildItem mapper, // ordering!
            Optional<ProvidedAmazonLambdaHandlerBuildItem> providedLambda,
            AmazonLambdaStaticRecorder recorder,
            RecorderContext context) {
        // can set handler within static initialization if only one handler exists in deployment
        if (providedLambda.isPresent()) {
            boolean useStreamHandler = false;
            for (Class handleInterface : providedLambda.get().getHandlerClass().getInterfaces()) {
                if (handleInterface.getName().equals(RequestStreamHandler.class.getName())) {
                    useStreamHandler = true;
                }
            }

            if (useStreamHandler) {
                Class<? extends RequestStreamHandler> handlerClass = (Class<? extends RequestStreamHandler>) context
                        .classProxy(providedLambda.get().getHandlerClass().getName());
                recorder.setStreamHandlerClass(handlerClass);
            } else {
                Class<? extends RequestHandler<?, ?>> handlerClass = (Class<? extends RequestHandler<?, ?>>) context
                        .classProxy(providedLambda.get().getHandlerClass().getName());

                recorder.setHandlerClass(handlerClass);
            }
        } else if (lambdas != null && lambdas.size() == 1) {
            AmazonLambdaBuildItem item = lambdas.get(0);
            if (item.isStreamHandler()) {
                Class<? extends RequestStreamHandler> handlerClass = (Class<? extends RequestStreamHandler>) context
                        .classProxy(item.getHandlerClass());
                recorder.setStreamHandlerClass(handlerClass);

            } else {
                Class<? extends RequestHandler<?, ?>> handlerClass = (Class<? extends RequestHandler<?, ?>>) context
                        .classProxy(item.getHandlerClass());

                recorder.setHandlerClass(handlerClass);

            }
        } else if (lambdas == null || lambdas.isEmpty()) {
            String errorMessage = "Unable to find handler class, make sure your deployment includes a single "
                    + RequestHandler.class.getName() + " or, " + RequestStreamHandler.class.getName() + " implementation";
            throw new RuntimeException(errorMessage);
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void recordBeanContainer(BeanContainerBuildItem beanContainerBuildItem,
            AmazonLambdaRecorder recorder,
            // try to order this after service recorders
            List<ServiceStartBuildItem> orderServicesFirst) {
        recorder.setBeanContainer(beanContainerBuildItem.getValue());

    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void recordHandlerClass(List<AmazonLambdaBuildItem> lambdas,
            Optional<ProvidedAmazonLambdaHandlerBuildItem> providedLambda,
            BeanContainerBuildItem beanContainerBuildItem,
            AmazonLambdaRecorder recorder,
            List<ServiceStartBuildItem> orderServicesFirst, // try to order this after service recorders
            RecorderContext context) {
        // Have to set lambda class at runtime if there is not a provided lambda or there is more than one lambda in
        // deployment
        if (!providedLambda.isPresent() && lambdas != null && lambdas.size() > 1) {
            List<Class<? extends RequestHandler<?, ?>>> unnamed = new ArrayList<>();
            Map<String, Class<? extends RequestHandler<?, ?>>> named = new HashMap<>();

            List<Class<? extends RequestStreamHandler>> unnamedStreamHandler = new ArrayList<>();
            Map<String, Class<? extends RequestStreamHandler>> namedStreamHandler = new HashMap<>();

            for (AmazonLambdaBuildItem i : lambdas) {
                if (i.isStreamHandler()) {
                    if (i.getName() == null) {
                        unnamedStreamHandler
                                .add((Class<? extends RequestStreamHandler>) context.classProxy(i.getHandlerClass()));
                    } else {
                        namedStreamHandler.put(i.getName(),
                                (Class<? extends RequestStreamHandler>) context.classProxy(i.getHandlerClass()));
                    }
                } else {
                    if (i.getName() == null) {
                        unnamed.add((Class<? extends RequestHandler<?, ?>>) context.classProxy(i.getHandlerClass()));
                    } else {
                        named.put(i.getName(), (Class<? extends RequestHandler<?, ?>>) context.classProxy(i.getHandlerClass()));
                    }
                }
            }

            recorder.chooseHandlerClass(unnamed, named, unnamedStreamHandler, namedStreamHandler);
        }
    }

    /**
     * This should only run when building a native image
     */
    @BuildStep(onlyIf = NativeBuild.class)
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void startPoolLoop(AmazonLambdaRecorder recorder,
            ShutdownContextBuildItem shutdownContextBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            List<ServiceStartBuildItem> orderServicesFirst // try to order this after service recorders
    ) {
        recorder.startPollLoop(shutdownContextBuildItem, launchModeBuildItem.getLaunchMode());
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void startPoolLoopDevOrTest(AmazonLambdaRecorder recorder,
            List<ServiceStartBuildItem> orderServicesFirst, // force some ordering of recorders
            ShutdownContextBuildItem shutdownContextBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {
        LaunchMode mode = launchModeBuildItem.getLaunchMode();
        if (mode.isDevOrTest()) {
            recorder.startPollLoop(shutdownContextBuildItem, mode);
        }
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    void recordExpectedExceptions(LambdaBuildTimeConfig config,
            BuildProducer<ReflectiveClassBuildItem> registerForReflection,
            AmazonLambdaStaticRecorder recorder) {
        Set<Class<?>> classes = config.expectedExceptions.map(Set::copyOf).orElseGet(Set::of);
        classes.stream()
                .map(clazz -> ReflectiveClassBuildItem.builder(clazz).constructors(false).build())
                .forEach(registerForReflection::produce);
        recorder.setExpectedExceptionClasses(classes);
    }

}
