package io.quarkus.mongodb.deployment;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.codecs.configuration.CodecProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.mongodb.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import io.quarkus.mongodb.runtime.MongoClientProducer;
import io.quarkus.mongodb.runtime.MongoClientRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class MongoClientProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerClientProducerBean() {
        return AdditionalBeanBuildItem.unremovableOf(MongoClientProducer.class);
    }

    @BuildStep
    CodecProviderBuildItem collectCodecProviders(CombinedIndexBuildItem indexBuildItem) {
        Collection<ClassInfo> codecProviderClasses = indexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(CodecProvider.class.getName()));
        List<String> names = codecProviderClasses.stream().map(ci -> ci.name().toString()).collect(Collectors.toList());
        return new CodecProviderBuildItem(names);
    }

    @BuildStep
    List<ReflectiveClassBuildItem> addCodecsToNative(CodecProviderBuildItem providers) {
        return providers.getCodecProviderClassNames().stream()
                .map(s -> new ReflectiveClassBuildItem(true, true, false, s))
                .collect(Collectors.toList());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    MongoClientBuildItem build(BuildProducer<FeatureBuildItem> feature, MongoClientRecorder recorder,
            BeanContainerBuildItem beanContainer, LaunchModeBuildItem launchMode,
            ShutdownContextBuildItem shutdown,
            MongoClientConfig config, CodecProviderBuildItem codecs,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.MONGODB_CLIENT));

        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.MONGODB_CLIENT));

        RuntimeValue<MongoClient> client = recorder.configureTheClient(config, beanContainer.getValue(),
                launchMode.getLaunchMode(), shutdown,
                codecs.getCodecProviderClassNames());
        RuntimeValue<ReactiveMongoClient> reactiveClient = recorder.configureTheReactiveClient();
        return new MongoClientBuildItem(client, reactiveClient);
    }

    @BuildStep
    HealthBuildItem addHealthCheck(MongoClientBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.mongodb.health.MongoHealthCheck",
                buildTimeConfig.healthEnabled, "mongodb");
    }
}
