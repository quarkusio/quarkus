package io.quarkus.kafka.client.deployment;

import java.util.HashSet;
import java.util.Set;

import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.clients.consumer.StickyAssignor;
import org.apache.kafka.clients.consumer.internals.PartitionAssignor;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.ByteBufferDeserializer;
import org.apache.kafka.common.serialization.ByteBufferSerializer;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.DoubleDeserializer;
import org.apache.kafka.common.serialization.DoubleSerializer;
import org.apache.kafka.common.serialization.FloatDeserializer;
import org.apache.kafka.common.serialization.FloatSerializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.ShortDeserializer;
import org.apache.kafka.common.serialization.ShortSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.kafka.client.serialization.JsonbDeserializer;
import io.quarkus.kafka.client.serialization.JsonbSerializer;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class KafkaProcessor {

    static final Class[] BUILT_INS = {
            //serializers
            ShortSerializer.class,
            DoubleSerializer.class,
            LongSerializer.class,
            BytesSerializer.class,
            ByteArraySerializer.class,
            IntegerSerializer.class,
            ByteBufferSerializer.class,
            StringSerializer.class,
            FloatSerializer.class,

            //deserializers
            ShortDeserializer.class,
            DoubleDeserializer.class,
            LongDeserializer.class,
            BytesDeserializer.class,
            ByteArrayDeserializer.class,
            IntegerDeserializer.class,
            ByteBufferDeserializer.class,
            StringDeserializer.class,
            FloatDeserializer.class,
    };

    @BuildStep
    public void build(CombinedIndexBuildItem indexBuildItem, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<JniBuildItem> jni, Capabilities capabilities) {
        Set<ClassInfo> toRegister = new HashSet<>();

        toRegister.addAll(indexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(Serializer.class.getName())));
        toRegister.addAll(indexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(Deserializer.class.getName())));
        toRegister.addAll(indexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(Partitioner.class.getName())));
        toRegister.addAll(indexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(PartitionAssignor.class.getName())));

        for (Class i : BUILT_INS) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, i.getName()));
            toRegister.addAll(indexBuildItem.getIndex()
                    .getAllKnownSubclasses(DotName.createSimple(i.getName())));
        }
        if (capabilities.isCapabilityPresent(Capabilities.JSONB)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, JsonbSerializer.class, JsonbDeserializer.class));
            toRegister.addAll(indexBuildItem.getIndex()
                    .getAllKnownSubclasses(DotName.createSimple(JsonbSerializer.class.getName())));
            toRegister.addAll(indexBuildItem.getIndex()
                    .getAllKnownSubclasses(DotName.createSimple(JsonbDeserializer.class.getName())));
        }
        if (capabilities.isCapabilityPresent(Capabilities.JACKSON)) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(false, false, ObjectMapperSerializer.class, ObjectMapperDeserializer.class));
            toRegister.addAll(indexBuildItem.getIndex()
                    .getAllKnownSubclasses(DotName.createSimple(ObjectMapperSerializer.class.getName())));
            toRegister.addAll(indexBuildItem.getIndex()
                    .getAllKnownSubclasses(DotName.createSimple(ObjectMapperDeserializer.class.getName())));
        }

        for (ClassInfo s : toRegister) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, s.toString()));
        }

        // built in partitioner and partition assignors
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, DefaultPartitioner.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, RangeAssignor.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, RoundRobinAssignor.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, StickyAssignor.class.getName()));

        // enable JNI
        jni.produce(new JniBuildItem());
    }

    @BuildStep
    HealthBuildItem addHealthCheck(KafkaBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.kafka.client.health.KafkaHealthCheck",
                buildTimeConfig.healthEnabled, "kafka");
    }
}
