package io.quarkus.kafka;

import java.util.Arrays;
import java.util.Collection;

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

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

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
    public void build(CombinedIndexBuildItem indexBuildItem, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        Collection<ClassInfo> serializers = indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(Serializer.class.getName()));
        Collection<ClassInfo> deserializers = indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(Deserializer.class.getName()));
        Collection<ClassInfo> partitioners = indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(Partitioner.class.getName()));
        Collection<ClassInfo> partitionAssignors = indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(PartitionAssignor.class.getName()));

        for (Class i : BUILT_INS) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, i.getName()));
        }

        for (Collection<ClassInfo> list : Arrays.asList(serializers, deserializers, partitioners, partitionAssignors)) {
            for (ClassInfo s : list) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, s.toString()));
            }
        }

        // built in partitioner and partition assignors
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, DefaultPartitioner.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, RangeAssignor.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, RoundRobinAssignor.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, StickyAssignor.class.getName()));

    }
}
