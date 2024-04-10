package io.quarkus.grpc.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.grpc.transcoding.GrpcTranscodingContainer;
import io.quarkus.grpc.transcoding.GrpcTranscodingMethod;
import io.quarkus.grpc.transcoding.GrpcTranscodingRecorder;
import io.quarkus.grpc.transcoding.GrpcTranscodingServer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;

class GrpcTranscodingProcessor {

    private static final String FEATURE = "quarkus-grpc-transcoding";
    private static final Logger log = LoggerFactory.getLogger(GrpcTranscodingProcessor.class);

    private static final String[] PRODUCES = new String[] { "application/json" };
    private static final String[] CONSUMES = new String[] { "application/json" };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void processGeneratedBeans(CombinedIndexBuildItem index,
            BuildProducer<AnnotationsTransformerBuildItem> transformers,
            BuildProducer<GrpcTranscodingBuildItem> marshallings,
            BuildProducer<AdditionalBeanBuildItem> delegatingBeans) {
        Set<DotName> generatedBeans = new HashSet<>();

        for (ClassInfo generatedBean : index.getIndex().getAllKnownImplementors(GrpcDotNames.GRPC_TRANSCODING)) {
            DotName generatedBeanName = generatedBean.name();
            generatedBeans.add(generatedBeanName);

            // Extract gRPC transcoding configuration from methods and store the results
            List<GrpcTranscodingMethod> transcodingMethods = collectTranscodingMethods(generatedBean);
            marshallings.produce(new GrpcTranscodingBuildItem(generatedBeanName, transcodingMethods));
        }

        delegatingBeans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcTranscodingContainer.class));

        transformers.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                // Check if the class is a generated gRPC transcoding bean
                if (generatedBeans.contains(context.getTarget().asClass().name())) {
                    context.transform()
                            .add(BuiltinScope.SINGLETON.getName())
                            .done();
                }
            }
        }));
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    GrpcTranscodingServerBuildItem buildTranscoding(GrpcTranscodingRecorder recorder,
            VertxBuildItem vertx,
            VertxWebRouterBuildItem routerBuildItem,
            List<GrpcTranscodingBuildItem> marshallings,
            Capabilities capabilities,
            ShutdownContextBuildItem shutdown) {
        // Build a map to organize the collected gRPC transcoding methods by service name
        Map<String, List<GrpcTranscodingMethod>> methods = new HashMap<>();
        for (GrpcTranscodingBuildItem item : marshallings) {
            String name = item.getMarshallingClass().toString().replace("Marshalling", "");
            methods.put(name, item.getTranscodingMethods());
        }

        // Create and initialize the gRPC transcoding server
        RuntimeValue<GrpcTranscodingServer> server = recorder.initializeMarshallingServer(vertx.getVertx(),
                routerBuildItem.getHttpRouter(), shutdown, methods, capabilities.isPresent(Capability.SECURITY));
        return new GrpcTranscodingServerBuildItem(server);
    }

    private static List<GrpcTranscodingMethod> collectTranscodingMethods(ClassInfo service) {
        List<GrpcTranscodingMethod> transcodingMethods = new ArrayList<>();
        for (MethodInfo method : service.methods()) {
            if (method.hasAnnotation(GrpcDotNames.GRPC_METHOD)) {
                AnnotationInstance annotation = method.annotation(GrpcDotNames.GRPC_METHOD);

                GrpcTranscodingMethod transcodingMethod = new GrpcTranscodingMethod(
                        annotation.value("grpcMethodName").asString(), annotation.value("httpMethod").asString(),
                        annotation.value("httpPath").asString());
                transcodingMethods.add(transcodingMethod);
            }
        }

        return transcodingMethods;
    }
}
