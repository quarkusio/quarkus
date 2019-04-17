package io.quarkus.amazon.lambda.deployment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaTemplate;
import io.quarkus.amazon.lambda.runtime.FunctionError;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.recording.RecorderContext;

@SuppressWarnings("unchecked")
public final class AmazonLambdaProcessor {
    public static final String AWS_LAMBDA_EVENTS_ARCHIVE_MARKERS = "com/amazonaws/services/lambda/runtime/events";

    private static final DotName REQUEST_HANDLER = DotName.createSimple(RequestHandler.class.getName());

    @BuildStep(applicationArchiveMarkers = { AWS_LAMBDA_EVENTS_ARCHIVE_MARKERS })
    List<AmazonLambdaClassNameBuildItem> discover(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClasses) {
        List<AmazonLambdaClassNameBuildItem> ret = new ArrayList<>();

        for (ClassInfo info : combinedIndexBuildItem.getIndex().getAllKnownImplementors(REQUEST_HANDLER)) {
            final DotName name = info.name();

            final String lambda = name.toString();
            ret.add(new AmazonLambdaClassNameBuildItem(lambda));

            ClassInfo current = info;
            boolean done = false;
            while (current != null && !done) {
                for (MethodInfo method : current.methods()) {
                    if (method.name().equals("handleRequest")
                            && method.parameters().size() == 2
                            && !method.parameters().get(0).name().equals(DotName.createSimple(Object.class.getName()))) {
                        reflectiveClasses.produce(new ReflectiveHierarchyBuildItem(method.parameters().get(0)));
                        reflectiveClasses.produce(new ReflectiveHierarchyBuildItem(method.returnType()));
                        done = true;
                        break;
                    }
                }
                current = combinedIndexBuildItem.getIndex().getClassByName(current.superName());
            }
        }
        return ret;
    }

    @BuildStep
    ReflectiveClassBuildItem functionError() {
        return new ReflectiveClassBuildItem(true, true, FunctionError.class);
    }

    @BuildStep
    AdditionalBeanBuildItem beans(List<AmazonLambdaClassNameBuildItem> lambdas) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();
        for (AmazonLambdaClassNameBuildItem i : lambdas) {
            builder.addBeanClass(i.getClassName());
        }
        return builder.build();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    List<AmazonLambdaBuildItem> process(List<AmazonLambdaClassNameBuildItem> items,
            RecorderContext context,
            AmazonLambdaTemplate template) {
        List<AmazonLambdaBuildItem> ret = new ArrayList<>();
        for (AmazonLambdaClassNameBuildItem i : items) {
            ret.add(new AmazonLambdaBuildItem(i.getClassName(),
                    template.discoverParameterTypes((Class<? extends RequestHandler>) context.classProxy(i.getClassName()))));
        }
        return ret;
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
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
    public void servlets(List<AmazonLambdaBuildItem> lambdas,
            BeanContainerBuildItem beanContainerBuildItem,
            AmazonLambdaTemplate template,
            RecorderContext context,
            ShutdownContextBuildItem shutdownContextBuildItem) throws IOException {

        if (lambdas.isEmpty()) {
            return;
        } else if (lambdas.size() != 1) {
            throw new RuntimeException("More than one lambda discovered " + lambdas);
        }
        AmazonLambdaBuildItem lambda = lambdas.get(0);

        template.start((Class<? extends RequestHandler>) context.classProxy(lambda.getHandlerClass()), shutdownContextBuildItem,
                lambda.getTargetType(), beanContainerBuildItem.getValue());

    }
}
