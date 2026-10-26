package io.quarkus.amazon.lambda.deployment;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaMapperRecorder;
import io.quarkus.amazon.lambda.runtime.FunctionError;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.SnapStartDefaultValueBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.pkg.steps.NativeSourcesBuild;
import io.quarkus.jackson.runtime.JsonMapperProducer;
import io.quarkus.runtime.LaunchMode;
import tools.jackson.databind.ObjectMapper;

public final class AmazonLambdaCommonProcessor {

    static final String OBJECT_MAPPER_SERVICE = "io.quarkus.amazon-lambda.object-mapper";

    @BuildStep
    public SnapStartDefaultValueBuildItem enableSnapStartByDefault() {
        return new SnapStartDefaultValueBuildItem(true);
    }

    @BuildStep(onlyIf = NativeSourcesBuild.class)
    void failForNativeSources(BuildProducer<ArtifactResultBuildItem> artifactResultProducer) {
        throw new IllegalArgumentException(
                "The Amazon Lambda extensions are incompatible with the 'native-sources' package type.");
    }

    @BuildStep
    void tmpdirs(BuildProducer<SystemPropertyBuildItem> systemProperty,
            LaunchModeBuildItem launchModeBuildItem) {
        LaunchMode mode = launchModeBuildItem.getLaunchMode();
        if (mode.isDevOrTest()) {
            return; // just in case we're on Windows.
        }
        systemProperty.produce(new SystemPropertyBuildItem("java.io.tmpdir", "/tmp"));
        systemProperty.produce(new SystemPropertyBuildItem("vertx.cacheDirBase", "/tmp/vertx"));
    }

    @BuildStep
    public void markObjectMapper(BuildProducer<UnremovableBeanBuildItem> unremovable) {
        unremovable.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion(ObjectMapper.class.getName())));
        unremovable.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion(JsonMapperProducer.class.getName())));
    }

    @BuildStep
    public LambdaObjectMapperInitializedBuildItem initObjectMapper(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar
                .forService(OBJECT_MAPPER_SERVICE)
                .atPhase(Phase.STATIC_INIT)
                .require(ArcContainer.class)
                .afterBuildItem(BeanContainerBuildItem.class) // make sure beanContainer is initialized
                .onStart((ctx, container) -> AmazonLambdaMapperRecorder.initObjectMapper(container));
        return new LambdaObjectMapperInitializedBuildItem();
    }

    @BuildStep(onlyIf = NativeBuild.class)
    public void initContextReaders(ServiceRegistrar serviceRegistrar) {
        // only need context readers in native or dev or test mode
        registerContextReaders(serviceRegistrar);
    }

    @BuildStep
    void initContextReaders(ServiceRegistrar serviceRegistrar,
            LaunchModeBuildItem launchModeBuildItem) {
        LaunchMode mode = launchModeBuildItem.getLaunchMode();
        if (mode.isDevOrTest()) {
            // only need context readers in native or dev or test mode
            registerContextReaders(serviceRegistrar);
        }
    }

    private static void registerContextReaders(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar
                .forService("io.quarkus.amazon-lambda.context-readers")
                .atPhase(Phase.STATIC_INIT)
                .after(OBJECT_MAPPER_SERVICE)
                .onStart(ctx -> AmazonLambdaMapperRecorder.initContextReaders());
    }

    @BuildStep
    public void registerForSerialization(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                FunctionError.class.getName()).build());
    }

}
