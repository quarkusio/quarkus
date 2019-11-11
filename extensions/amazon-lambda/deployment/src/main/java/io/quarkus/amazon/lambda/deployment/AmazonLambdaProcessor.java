package io.quarkus.amazon.lambda.deployment;

import java.lang.reflect.Method;
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

import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaRecorder;
import io.quarkus.amazon.lambda.runtime.FunctionError;
import io.quarkus.amazon.lambda.runtime.LambdaBuildTimeConfig;
import io.quarkus.amazon.lambda.runtime.LambdaConfig;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.BuildException;
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
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.LaunchMode;

@SuppressWarnings("unchecked")
public final class AmazonLambdaProcessor {
    public static final String AWS_LAMBDA_EVENTS_ARCHIVE_MARKERS = "com/amazonaws/services/lambda/runtime/events";

    private static final DotName REQUEST_HANDLER = DotName.createSimple(RequestHandler.class.getName());

    private static final DotName NAMED = DotName.createSimple(Named.class.getName());
    private static final Logger log = Logger.getLogger(AmazonLambdaProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.AMAZON_LAMBDA);
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
        if (allKnownImplementors.size() > 0 && providedLambda.isPresent()) {
            throw new BuildException(
                    "Multiple handler classes.  You have a custom handler class and the " + providedLambda.get().getProvider()
                            + " extension.  Please remove one of them from your deployment.",
                    Collections.emptyList());

        }
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();
        List<AmazonLambdaBuildItem> ret = new ArrayList<>();

        for (ClassInfo info : allKnownImplementors) {
            final DotName name = info.name();
            builder.addBeanClass(name.toString());
            String cdiName = null;
            List<AnnotationInstance> named = info.annotations().get(NAMED);
            if (named != null && !named.isEmpty()) {
                cdiName = named.get(0).value().asString();
            }

            final String lambda = name.toString();
            ret.add(new AmazonLambdaBuildItem(lambda, cdiName));
            reflectiveClassBuildItemBuildProducer.produce(new ReflectiveClassBuildItem(true, false, lambda));

            ClassInfo current = info;
            boolean done = false;
            while (current != null && !done) {
                for (MethodInfo method : current.methods()) {
                    if (method.name().equals("handleRequest")
                            && method.parameters().size() == 2
                            && !method.parameters().get(0).name().equals(DotName.createSimple(Object.class.getName()))) {
                        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(method.parameters().get(0)));
                        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(method.returnType()));
                        done = true;
                        break;
                    }
                }
                current = combinedIndexBuildItem.getIndex().getClassByName(current.superName());
            }
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
            Class<? extends RequestHandler<?, ?>> handlerClass = (Class<? extends RequestHandler<?, ?>>) context
                    .classProxy(providedLambda.get().getHandlerClass().getName());
            recorder.setHandlerClass(handlerClass, beanContainerBuildItem.getValue());

        } else if (lambdas != null) {
            List<Class<? extends RequestHandler<?, ?>>> unnamed = new ArrayList<>();
            Map<String, Class<? extends RequestHandler<?, ?>>> named = new HashMap<>();
            for (AmazonLambdaBuildItem i : lambdas) {
                if (i.getName() == null) {
                    unnamed.add((Class<? extends RequestHandler<?, ?>>) context.classProxy(i.getHandlerClass()));
                } else {
                    named.put(i.getName(), (Class<? extends RequestHandler<?, ?>>) context.classProxy(i.getHandlerClass()));
                }
            }
            recorder.chooseHandlerClass(unnamed, named, beanContainerBuildItem.getValue(), config);
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

    /**
     * Lambda custom runtime does not like ipv6.
     */
    @BuildStep(onlyIf = NativeBuild.class)
    void ipv4Only(BuildProducer<SystemPropertyBuildItem> systemProperty) {
        // lambda custom runtime does not like IPv6
        systemProperty.produce(new SystemPropertyBuildItem("java.net.preferIPv4Stack", "true"));
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void enableNativeEventLoop(LambdaBuildTimeConfig config,
            AmazonLambdaRecorder recorder,
            List<ServiceStartBuildItem> orderServicesFirst, // force some ordering of recorders
            ShutdownContextBuildItem shutdownContextBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {
        LaunchMode mode = launchModeBuildItem.getLaunchMode();
        if (config.enablePollingJvmMode && mode.isDevOrTest()) {
            recorder.startPollLoop(shutdownContextBuildItem);
        }
    }

}
