package org.jboss.shamrock.reactivemessaging;

import io.vertx.kafka.client.serialization.JsonObjectDeserializer;
import io.vertx.kafka.client.serialization.JsonObjectSerializer;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;

import java.util.Collection;

public class KafkaCodecProcessor {

    @BuildStep
    public void build(CombinedIndexBuildItem indexBuildItem, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        Collection<ClassInfo> serializers = indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(Serializer.class.getName()));
        Collection<ClassInfo> deserializers = indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(Deserializer.class.getName()));

        // TODO For some reason it does not seem to work:

        for (ClassInfo s : serializers) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, s.toString()));
        }

        for (ClassInfo s : deserializers) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, s.toString()));
        }

        // Explicit list them because of issue mentioned above ^
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, StringSerializer.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, JsonObjectSerializer.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, StringDeserializer.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, JsonObjectDeserializer.class.getName()));


        // Also
        // Kafka has is heavily using reflection - at least these 2 classes are instantiated
        // The first to produce
        // The second to consume
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, DefaultPartitioner.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, RangeAssignor.class.getName()));

    }
}
