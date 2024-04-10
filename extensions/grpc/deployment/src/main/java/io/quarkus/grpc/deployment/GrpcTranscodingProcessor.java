package io.quarkus.grpc.deployment;

import static io.quarkus.deployment.Feature.GRPC_TRANSCODING;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

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
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.grpc.runtime.config.GrpcTranscodingConfig;
import io.quarkus.grpc.transcoding.GrpcTranscodingContainer;
import io.quarkus.grpc.transcoding.GrpcTranscodingMethod;
import io.quarkus.grpc.transcoding.GrpcTranscodingRecorder;
import io.quarkus.grpc.transcoding.GrpcTranscodingServer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;

class GrpcTranscodingProcessor {

    @BuildStep
    void processGeneratedBeans(CombinedIndexBuildItem index,
            GrpcTranscodingConfig transcodingConfig,
            BuildProducer<FeatureBuildItem> features,
            BuildProducer<AnnotationsTransformerBuildItem> transformers,
            BuildProducer<GrpcTranscodingBuildItem> marshallings,
            BuildProducer<AdditionalBeanBuildItem> delegatingBeans) {
        // Check if the gRPC transcoding feature is enabled
        if (!transcodingConfig.enabled) {
            return;
        }

        Map<DotName, List<GrpcTranscodingMethod>> methods = new HashMap<>();

        for (ClassInfo generatedBean : index.getIndex().getAllKnownImplementors(GrpcDotNames.GRPC_TRANSCODING)) {
            // Extract gRPC transcoding configuration from methods and store the results
            List<GrpcTranscodingMethod> transcodingMethods = collectTranscodingMethods(generatedBean);
            methods.put(generatedBean.name(), transcodingMethods);
        }

        if (methods.isEmpty()) {
            return;
        }

        for (Map.Entry<DotName, List<GrpcTranscodingMethod>> entry : methods.entrySet()) {
            marshallings.produce(new GrpcTranscodingBuildItem(entry.getKey(), entry.getValue()));
        }

        features.produce(new FeatureBuildItem(GRPC_TRANSCODING));
        delegatingBeans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcTranscodingContainer.class));

        Set<DotName> generatedBeans = methods.keySet();

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
    ServiceStartBuildItem buildTranscoding(GrpcTranscodingRecorder recorder,
            VertxBuildItem vertx,
            VertxWebRouterBuildItem routerBuildItem,
            List<GrpcTranscodingBuildItem> marshallings,
            Capabilities capabilities,
            BuildProducer<GrpcTranscodingServerBuildItem> transcodingServer,
            ShutdownContextBuildItem shutdown) {
        // Build a map to organize the collected gRPC transcoding methods by service name
        Map<String, List<GrpcTranscodingMethod>> methods = new HashMap<>();
        for (GrpcTranscodingBuildItem item : marshallings) {
            String name = item.getMarshallingClass().toString().replace("Marshalling", "");
            methods.put(name, item.getTranscodingMethods());
        }

        if (methods.isEmpty()) {
            return null;
        }

        // Create and initialize the gRPC transcoding server
        RuntimeValue<GrpcTranscodingServer> server = recorder.initializeMarshallingServer(vertx.getVertx(),
                routerBuildItem.getHttpRouter(), shutdown, methods, capabilities.isPresent(Capability.SECURITY));

        transcodingServer.produce(new GrpcTranscodingServerBuildItem(server));
        return new ServiceStartBuildItem(GRPC_TRANSCODING);
    }

    private static List<GrpcTranscodingMethod> collectTranscodingMethods(ClassInfo service) {
        List<GrpcTranscodingMethod> transcodingMethods = new ArrayList<>();
        for (MethodInfo method : service.methods()) {
            if (method.hasAnnotation(GrpcDotNames.GRPC_TRANSCODING_METHOD)) {
                AnnotationInstance annotation = method.annotation(GrpcDotNames.GRPC_TRANSCODING_METHOD);

                GrpcTranscodingMethod transcodingMethod = new GrpcTranscodingMethod(
                        annotation.value("grpcMethodName").asString(), annotation.value("httpMethod").asString(),
                        annotation.value("httpPath").asString());
                transcodingMethods.add(transcodingMethod);
            }
        }

        return transcodingMethods;
    }
}
