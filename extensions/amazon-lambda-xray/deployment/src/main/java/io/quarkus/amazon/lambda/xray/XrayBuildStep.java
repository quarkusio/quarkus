package io.quarkus.amazon.lambda.xray;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;

public class XrayBuildStep {
    @BuildStep(onlyIf = NativeBuild.class)
    public void process(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized,
            BuildProducer<NativeImageResourceBuildItem> resource) {
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("com.amazonaws.xray.AWSXRay"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("com.amazonaws.xray.AWSXRayRecorder"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("com.amazonaws.xray.interceptors.TracingInterceptor"));
        runtimeInitialized.produce(
                new RuntimeInitializedClassBuildItem("com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("com.amazonaws.xray.ThreadLocalStorage"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(
                true, true, true,
                "com.amazonaws.xray.handlers.config.AWSServiceHandlerManifest",
                "com.amazonaws.xray.AWSXRay",
                "com.amazonaws.xray.strategy.sampling.manifest.SamplingRuleManifest",
                "com.amazonaws.xray.strategy.sampling.rule.SamplingRule",
                "com.amazonaws.xray.strategy.sampling.reservoir.Reservoir",
                "com.amazonaws.xray.strategy.sampling.reservoir.Reservoir$MaxFunction",
                "com.amazonaws.xray.strategy.sampling.reservoir.Reservoir$LessThan10",
                "com.amazonaws.xray.strategy.sampling.reservoir.Reservoir$AtLeast10",
                "com.amazonaws.auth.AWS4Signer",
                "com.amazonaws.xray.handlers.config.AWSOperationHandlerManifest",
                "com.amazonaws.xray.handlers.config.AWSOperationHandler",
                "com.amazonaws.xray.handlers.config.AWSOperationHandlerRequestDescriptor",
                "com.amazonaws.xray.handlers.config.AWSOperationHandlerResponseDescriptor",
                "com.amazonaws.xray.entities.ThrowableDescription",
                "com.amazonaws.xray.entities.SubsegmentImpl",
                "com.amazonaws.xray.entities.EntityImpl",
                "com.amazonaws.xray.entities.TraceID",
                "com.amazonaws.xray.entities.Cause",
                "com.amazonaws.xray.entities.SegmentImpl",
                "com.fasterxml.jackson.databind.ser.std.ToStringSerializer"));

        resource.produce(new NativeImageResourceBuildItem(
                "com/amazonaws/xray/interceptors/DefaultOperationParameterWhitelist.json",
                "com/amazonaws/xray/strategy/sampling/DefaultSamplingRules.json",
                "com/amazonaws/xray/sdk.properties"));

        //Register Apache client
        proxyDefinition.produce(
                new NativeImageProxyDefinitionBuildItem("org.apache.http.conn.HttpClientConnectionManager",
                        "org.apache.http.pool.ConnPoolControl",
                        "com.amazonaws.http.conn.Wrapped"));
    }
}
