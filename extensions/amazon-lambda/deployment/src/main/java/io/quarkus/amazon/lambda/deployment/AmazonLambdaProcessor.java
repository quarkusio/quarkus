package io.quarkus.amazon.lambda.deployment;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaNativeRecorder;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaRecorder;
import io.quarkus.amazon.lambda.runtime.FunctionError;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedSubstrateClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.LaunchMode;

@SuppressWarnings("unchecked")
public final class AmazonLambdaProcessor {
    public static final String AWS_LAMBDA_EVENTS_ARCHIVE_MARKERS = "com/amazonaws/services/lambda/runtime/events";

    private static final DotName REQUEST_HANDLER = DotName.createSimple(RequestHandler.class.getName());

    private static final DotName NAMED = DotName.createSimple(Named.class.getName());
    private static final Logger log = Logger.getLogger(AmazonLambdaProcessor.class);

    @BuildStep(applicationArchiveMarkers = { AWS_LAMBDA_EVENTS_ARCHIVE_MARKERS })
    void discover(CombinedIndexBuildItem combinedIndexBuildItem,
            Optional<ProvidedAmazonLambdaHandlerBuildItem> providedLambda,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            LambdaConfig config,
            BuildProducer<AmazonLambdaBuildItem> lambdaProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer) throws BuildException {

        Collection<ClassInfo> allKnownImplementors = combinedIndexBuildItem.getIndex().getAllKnownImplementors(REQUEST_HANDLER);
        if (allKnownImplementors.size() > 0 && providedLambda.isPresent()) {
            throw new BuildException(
                    "Multiple handler classes.  You have a custom handler class and the " + providedLambda.get().getProvider()
                            + " extension.  Please remove one of them from your deployment.",
                    Collections.emptyList());

        }

        if (providedLambda.isPresent()) {
            processProvidedLambda(providedLambda.get(),
                    additionalBeanBuildItemBuildProducer,
                    lambdaProducer,
                    reflectiveClassBuildItemBuildProducer);

        } else {
            processCustomHandler(combinedIndexBuildItem,
                    additionalBeanBuildItemBuildProducer,
                    reflectiveHierarchy,
                    config, lambdaProducer,
                    reflectiveClassBuildItemBuildProducer, allKnownImplementors);
        }
        reflectiveClassBuildItemBuildProducer
                .produce(new ReflectiveClassBuildItem(true, true, true, FunctionError.class));

    }

    private void processProvidedLambda(ProvidedAmazonLambdaHandlerBuildItem providedLambda,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer,
            BuildProducer<AmazonLambdaBuildItem> lambdaProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();
        builder.addBeanClass(providedLambda.getHandlerClass());
        additionalBeanBuildItemBuildProducer.produce(builder.build());

        reflectiveClassBuildItemBuildProducer
                .produce(new ReflectiveClassBuildItem(true, true, true, providedLambda.getHandlerClass()));

        // TODO

        // This really isn't good enough.  We should recursively add reflection for all method and field types of the parameter
        // and return type.  Otherwise Jackson won't work.  In AWS Lambda HTTP extension, the whole jackson model is registered
        // for reflection.  Shouldn't have to do this.
        for (Method method : providedLambda.getHandlerClass().getMethods()) {
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

        lambdaProducer.produce(new AmazonLambdaBuildItem(providedLambda.getHandlerClass().getName(), null));
    }

    private void processCustomHandler(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            LambdaConfig config,
            BuildProducer<AmazonLambdaBuildItem> lambdaProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            Collection<ClassInfo> allKnownImplementors) throws BuildException {

        if (allKnownImplementors.size() > 1) {
            if (!config.handler.isPresent()) {
                throw new BuildException(
                        "Multiple handler classes, either specify the quarkus.lambda.handler property, or make sure there is only a single"
                                + RequestHandler.class.getName() + " implementation in the deployment",
                        Collections.emptyList());
            }
        }

        AmazonLambdaBuildItem lambdaBuildItem = null;
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();
        for (ClassInfo info : allKnownImplementors) {
            final DotName name = info.name();
            final String lambda = name.toString();

            builder.addBeanClass(name.toString());
            String cdiName = null;
            List<AnnotationInstance> named = info.annotations().get(NAMED);
            if (named != null && !named.isEmpty()) {
                cdiName = named.get(0).value().asString();
            }
            if (config.handler.isPresent() && !config.handler.get().equals(cdiName))
                continue;

            lambdaBuildItem = new AmazonLambdaBuildItem(lambda, cdiName);
            reflectiveClassBuildItemBuildProducer.produce(new ReflectiveClassBuildItem(true, true, true, lambda));

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
            break;
        }
        if (lambdaBuildItem == null) {
            if (config.handler.isPresent()) {
                throw new BuildException("Unable to find handler class with name " + config.handler.get()
                        + " make sure there is a handler class in the deployment with the correct @Named annotation",
                        Collections.emptyList());
            }
        } else {
            lambdaProducer.produce(lambdaBuildItem);
            additionalBeanBuildItemBuildProducer.produce(builder.build());
        }
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void setHandlerClassForNonNative(RecorderContext context, AmazonLambdaRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem,
            AmazonLambdaBuildItem lambda) {
        if (lambda == null)
            return;
        Class<? extends RequestHandler<?, ?>> handlerClass = (Class<? extends RequestHandler<?, ?>>) context
                .classProxy(lambda.getHandlerClass());
        recorder.setHandlerClass(handlerClass, beanContainerBuildItem.getValue());
    }

    /**
     * This should only run when building a native image
     */
    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT, optional = true)
    void bootNativeEventLoop(AmazonLambdaNativeRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem,
            RecorderContext context,
            ShutdownContextBuildItem shutdownContextBuildItem,
            List<ServiceStartBuildItem> orderServicesFirst, // try to force some ordering of recorders
            BuildProducer<GeneratedSubstrateClassBuildItem> substrate, // hax - todo find better way to run only on native image build
            AmazonLambdaBuildItem lambda) {
        startLoop(recorder, beanContainerBuildItem, context, shutdownContextBuildItem, lambda);
    }

    private void startLoop(AmazonLambdaNativeRecorder recorder, BeanContainerBuildItem beanContainerBuildItem,
            RecorderContext context, ShutdownContextBuildItem shutdownContextBuildItem, AmazonLambdaBuildItem lambda) {
        Class<? extends RequestHandler<?, ?>> handlerClass = (Class<? extends RequestHandler<?, ?>>) context
                .classProxy(lambda.getHandlerClass());
        recorder.startNativePollLoop(handlerClass, beanContainerBuildItem.getValue(), shutdownContextBuildItem);
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void enableNativeEventLoop(LambdaConfig config,
            AmazonLambdaNativeRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem,
            RecorderContext context,
            List<ServiceStartBuildItem> orderServicesFirst, // try to force some ordering of recorders
            ShutdownContextBuildItem shutdownContextBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            AmazonLambdaBuildItem lambda) {
        LaunchMode mode = launchModeBuildItem.getLaunchMode();
        if (config.enablePollingJvmMode && mode.isDevOrTest()) {
            startLoop(recorder, beanContainerBuildItem, context, shutdownContextBuildItem, lambda);
        }
    }

}
