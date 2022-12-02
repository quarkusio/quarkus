package io.quarkus.amazon.lambda.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaMapperRecorder;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CracDefaultValueBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.pkg.steps.NativeSourcesBuild;
import io.quarkus.jackson.runtime.ObjectMapperProducer;
import io.quarkus.runtime.LaunchMode;

@SuppressWarnings("unchecked")
public final class AmazonLambdaCommonProcessor {
    @BuildStep
    public CracDefaultValueBuildItem enableCracByDefault() {
        return new CracDefaultValueBuildItem(true);
    }

    @BuildStep(onlyIf = NativeSourcesBuild.class)
    void failForNativeSources(BuildProducer<ArtifactResultBuildItem> artifactResultProducer) {
        throw new IllegalArgumentException(
                "The Amazon Lambda extensions are incompatible with the 'native-sources' package type.");
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
                new UnremovableBeanBuildItem.BeanClassNameExclusion(ObjectMapperProducer.class.getName())));
    }

    @BuildStep()
    @Record(STATIC_INIT)
    public LambdaObjectMapperInitializedBuildItem initObjectMapper(BeanContainerBuildItem beanContainer, // make sure beanContainer is initialized
            AmazonLambdaMapperRecorder recorder) {
        recorder.initObjectMapper();
        return new LambdaObjectMapperInitializedBuildItem();
    }

    @BuildStep(onlyIf = NativeBuild.class)
    @Record(STATIC_INIT)
    public void initContextReaders(AmazonLambdaMapperRecorder recorder,
            LambdaObjectMapperInitializedBuildItem dependency) {
        // only need context readers in native or dev or test mode
        recorder.initContextReaders();
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    void initContextReaders(AmazonLambdaMapperRecorder recorder,
            LambdaObjectMapperInitializedBuildItem dependency,
            LaunchModeBuildItem launchModeBuildItem) {
        LaunchMode mode = launchModeBuildItem.getLaunchMode();
        if (mode.isDevOrTest()) {
            // only need context readers in native or dev or test mode
            recorder.initContextReaders();
        }
    }

}
