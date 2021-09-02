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

import javax.inject.Named;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;
import org.joda.time.DateTime;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaRecorder;
import io.quarkus.amazon.lambda.runtime.FunctionError;
import io.quarkus.amazon.lambda.runtime.LambdaConfig;
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
            reflectiveClassBuildItemBuildProducer.produce(new ReflectiveClassBuildItem(true, false, lambda));

            String cdiName = null;
            AnnotationInstance named = info.classAnnotation(NAMED);
            if (named != null) {
                cdiName = named.value().asString();
            }

            ClassInfo current = info;
            boolean done = false;
            boolean streamHandler = info.superName().equals(SKILL_STREAM_HANDLER) ? true : false;
            while (current != null && !done) {
                for (MethodInfo method : current.methods()) {
                    if (method.name().equals("handleRequest")) {
                        if (method.parameters().size() == 3) {
                            streamHandler = true;
                            done = true;
                            break;
                        } else if (method.parameters().size() == 2
                                && !method.parameters().get(0).name().equals(DotName.createSimple(Object.class.getName()))) {
                            String source = getClass().getSimpleName() + " > " + method.declaringClass() + "[" + method + "]";

                            reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                                    .type(method.parameters().get(0))
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
                .produce(new ReflectiveClassBuildItem(true, true, true, FunctionError.class));
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
                .produce(new ReflectiveClassBuildItem(true, true, true, handlerClass));

        // TODO
        // This really isn't good enough.  We should recursively add reflection for all method and field types of the parameter
        // and return type.  Otherwise Jackson won't work.  In AWS Lambda HTTP extension, the whole jackson model is registered
        // for reflection.  Shouldn't have to do this.
        for (Method method : handlerClass.getMethods()) {
            if (method.getName().equals("handleRequest")
                    && method.getParameterTypes().length == 2
                    && !method.getParameterTypes()[0].equals(Object.class)) {
                reflectiveClassBuildItemBuildProducer
                        .produce(new ReflectiveClassBuildItem(true, true, true, method.getParameterTypes()[0].getName()));
                reflectiveClassBuildItemBuildProducer
                        .produce(new ReflectiveClassBuildItem(true, true, true, method.getReturnType().getName()));
                reflectiveClassBuildItemBuildProducer.produce(new ReflectiveClassBuildItem(true, true, true,
                        DateTime.class));
                break;
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void recordHandlerClass(List<AmazonLambdaBuildItem> lambdas,
            Optional<ProvidedAmazonLambdaHandlerBuildItem> providedLambda,
            BeanContainerBuildItem beanContainerBuildItem,
            AmazonLambdaRecorder recorder,
            LambdaConfig config,
            List<ServiceStartBuildItem> orderServicesFirst, // try to order this after service recorders
            RecorderContext context) {
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
                recorder.setStreamHandlerClass(handlerClass, beanContainerBuildItem.getValue());
            } else {
                Class<? extends RequestHandler<?, ?>> handlerClass = (Class<? extends RequestHandler<?, ?>>) context
                        .classProxy(providedLambda.get().getHandlerClass().getName());

                recorder.setHandlerClass(handlerClass, beanContainerBuildItem.getValue());
            }
        } else if (lambdas != null) {
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

            recorder.chooseHandlerClass(unnamed, named, unnamedStreamHandler, namedStreamHandler,
                    beanContainerBuildItem.getValue(), config);
        }
    }

    /**
     * This should only run when building a native image
     */
    @BuildStep(onlyIf = NativeBuild.class)
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void startPoolLoop(AmazonLambdaRecorder recorder,
            ShutdownContextBuildItem shutdownContextBuildItem,
            List<ServiceStartBuildItem> orderServicesFirst // try to order this after service recorders
    ) {
        recorder.startPollLoop(shutdownContextBuildItem);
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void startPoolLoopDevOrTest(AmazonLambdaRecorder recorder,
            List<ServiceStartBuildItem> orderServicesFirst, // force some ordering of recorders
            ShutdownContextBuildItem shutdownContextBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {
        LaunchMode mode = launchModeBuildItem.getLaunchMode();
        if (mode.isDevOrTest()) {
            recorder.startPollLoop(shutdownContextBuildItem);
        }
    }

}
