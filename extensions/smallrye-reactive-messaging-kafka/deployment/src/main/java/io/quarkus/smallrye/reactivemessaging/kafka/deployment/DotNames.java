package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import org.jboss.jandex.DotName;

final class DotNames {
    // @formatter:off
    static final DotName INCOMING = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Incoming.class.getName());
    static final DotName OUTGOING = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Outgoing.class.getName());
    static final DotName CHANNEL = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Channel.class.getName());

    static final DotName EMITTER = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Emitter.class.getName());
    static final DotName MUTINY_EMITTER = DotName.createSimple(io.smallrye.reactive.messaging.MutinyEmitter.class.getName());

    static final DotName MESSAGE = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Message.class.getName());
    static final DotName KAFKA_RECORD = DotName.createSimple(io.smallrye.reactive.messaging.kafka.KafkaRecord.class.getName());
    static final DotName RECORD = DotName.createSimple(io.smallrye.reactive.messaging.kafka.Record.class.getName());
    static final DotName CONSUMER_RECORD = DotName.createSimple(org.apache.kafka.clients.consumer.ConsumerRecord.class.getName());
    static final DotName PRODUCER_RECORD = DotName.createSimple(org.apache.kafka.clients.producer.ProducerRecord.class.getName());

    static final DotName COMPLETION_STAGE = DotName.createSimple(java.util.concurrent.CompletionStage.class.getName());
    static final DotName UNI = DotName.createSimple(io.smallrye.mutiny.Uni.class.getName());

    static final DotName SUBSCRIBER = DotName.createSimple(org.reactivestreams.Subscriber.class.getName());
    static final DotName SUBSCRIBER_BUILDER = DotName.createSimple(org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder.class.getName());
    static final DotName PUBLISHER = DotName.createSimple(org.reactivestreams.Publisher.class.getName());
    static final DotName PUBLISHER_BUILDER = DotName.createSimple(org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder.class.getName());
    static final DotName PROCESSOR = DotName.createSimple(org.reactivestreams.Processor.class.getName());
    static final DotName PROCESSOR_BUILDER = DotName.createSimple(org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder.class.getName());
    static final DotName MULTI = DotName.createSimple(io.smallrye.mutiny.Multi.class.getName());

    static final DotName AVRO_GENERATED = DotName.createSimple("org.apache.avro.specific.AvroGenerated");
    static final DotName AVRO_GENERIC_RECORD = DotName.createSimple("org.apache.avro.generic.GenericRecord");
    static final DotName OBJECT_MAPPER_DESERIALIZER = DotName.createSimple(io.quarkus.kafka.client.serialization.ObjectMapperDeserializer.class.getName());
    static final DotName OBJECT_MAPPER_SERIALIZER = DotName.createSimple(io.quarkus.kafka.client.serialization.ObjectMapperSerializer.class.getName());
    static final DotName JSONB_DESERIALIZER = DotName.createSimple(io.quarkus.kafka.client.serialization.JsonbDeserializer.class.getName());
    static final DotName JSONB_SERIALIZER = DotName.createSimple(io.quarkus.kafka.client.serialization.JsonbSerializer.class.getName());

    static final DotName LIST = DotName.createSimple(java.util.List.class.getName());
    static final DotName KAFKA_BATCH_RECORD = DotName.createSimple(io.smallrye.reactive.messaging.kafka.KafkaRecordBatch.class.getName());
    static final DotName CONSUMER_RECORDS = DotName.createSimple(org.apache.kafka.clients.consumer.ConsumerRecords.class.getName());
    // @formatter:on
}
