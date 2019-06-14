package io.quarkus.mongo.deployment;

import java.util.Collection;
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
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.mongo.runtime.MongoClientConfig;
import io.quarkus.mongo.runtime.MongoClientProducer;
import io.quarkus.mongo.runtime.MongoClientTemplate;
import io.quarkus.runtime.RuntimeValue;

public class MongoClientProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerClients() {
        return AdditionalBeanBuildItem.unremovableOf(MongoClientProducer.class);
    }

    @BuildStep
    CodecProviderBuildItem collectionCodecs(CombinedIndexBuildItem indexBuildItem) {
        Collection<ClassInfo> codecProviderClasses = indexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(CodecProvider.class.getName()));
        return new CodecProviderBuildItem(
                codecProviderClasses.stream().map(ClassInfo::toString).collect(Collectors.toList()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    MongoClientBuildItem build(BuildProducer<FeatureBuildItem> feature, MongoClientTemplate template,
            BeanContainerBuildItem beanContainer, LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdown,
            MongoClientConfig config, CodecProviderBuildItem codecs) {
        feature.produce(new FeatureBuildItem("mongodb"));

        RuntimeValue<MongoClient> client = template.configureTheClient(config, beanContainer.getValue(),
                launchMode.getLaunchMode(), shutdown,
                codecs.getCodecProviderClassNames());
        return new MongoClientBuildItem(client);
    }
}
