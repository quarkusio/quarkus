package io.quarkus.amazon.lambda.deployment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaRecorder;
import io.quarkus.amazon.lambda.runtime.FunctionError;
import io.quarkus.amazon.lambda.runtime.LambdaConfig;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.recording.RecorderContext;

@SuppressWarnings("unchecked")
public final class AmazonLambdaProcessor {
    public static final String AWS_LAMBDA_EVENTS_ARCHIVE_MARKERS = "com/amazonaws/services/lambda/runtime/events";

    private static final DotName REQUEST_HANDLER = DotName.createSimple(RequestHandler.class.getName());

    private static final DotName NAMED = DotName.createSimple(Named.class.getName());

    @BuildStep(applicationArchiveMarkers = { AWS_LAMBDA_EVENTS_ARCHIVE_MARKERS })
    List<AmazonLambdaBuildItem> discover(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();
        List<AmazonLambdaBuildItem> ret = new ArrayList<>();

        for (ClassInfo info : combinedIndexBuildItem.getIndex().getAllKnownImplementors(REQUEST_HANDLER)) {
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
        return ret;
    }

    @BuildStep
    ReflectiveClassBuildItem functionError() {
        return new ReflectiveClassBuildItem(true, true, FunctionError.class);
    }

    @BuildStep
    void bootstrap(BuildProducer<GeneratedResourceBuildItem> generatedResources) throws IOException {
        try (final InputStream stream = getClass().getResourceAsStream("/bootstrap");
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[4096];
            int read;
            while ((read = stream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            generatedResources.produce(new GeneratedResourceBuildItem("bootstrap", outputStream.toByteArray()));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void processLambas(List<AmazonLambdaBuildItem> lambdas,
            BeanContainerBuildItem beanContainerBuildItem,
            AmazonLambdaRecorder recorder,
            LambdaConfig config,
            RecorderContext context,
            LaunchModeBuildItem launchMode,
            ShutdownContextBuildItem shutdownContextBuildItem) throws IOException {
        List<Class<? extends RequestHandler<?, ?>>> unnamed = new ArrayList<>();
        Map<String, Class<? extends RequestHandler<?, ?>>> named = new HashMap<>();
        for (AmazonLambdaBuildItem i : lambdas) {
            if (i.getName() == null) {
                unnamed.add((Class<? extends RequestHandler<?, ?>>) context.classProxy(i.getHandlerClass()));
            } else {
                named.put(i.getName(), (Class<? extends RequestHandler<?, ?>>) context.classProxy(i.getHandlerClass()));
            }
        }
        recorder.start(unnamed, named,
                shutdownContextBuildItem, config, beanContainerBuildItem.getValue());

    }
}
