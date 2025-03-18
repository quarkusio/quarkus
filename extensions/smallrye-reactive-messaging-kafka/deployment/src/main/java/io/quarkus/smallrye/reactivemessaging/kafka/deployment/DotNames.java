package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import org.jboss.jandex.DotName;

final class DotNames {
    // @formatter:off
    static final DotName INSTANCE = DotName.createSimple(jakarta.enterprise.inject.Instance.class.getName());
    static final DotName INJECTABLE_INSTANCE = DotName.createSimple(io.quarkus.arc.InjectableInstance.class.getName());
    static final DotName PROVIDER = DotName.createSimple(jakarta.inject.Provider.class.getName());
    static final DotName INCOMING = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Incoming.class.getName());
    static final DotName INCOMINGS = DotName.createSimple(io.smallrye.reactive.messaging.annotations.Incomings.class.getName());
    static final DotName OUTGOING = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Outgoing.class.getName());
    static final DotName OUTGOINGS = DotName.createSimple(io.smallrye.reactive.messaging.annotations.Outgoings.class.getName());
    static final DotName CHANNEL = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Channel.class.getName());

    static final DotName EMITTER = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Emitter.class.getName());
    static final DotName MUTINY_EMITTER = DotName.createSimple(io.smallrye.reactive.messaging.MutinyEmitter.class.getName());
    static final DotName CONTEXTUAL_EMITTER = DotName.createSimple(io.quarkus.smallrye.reactivemessaging.runtime.ContextualEmitter.class.getName());
    static final DotName KAFKA_TRANSACTIONS_EMITTER = DotName.createSimple(io.smallrye.reactive.messaging.kafka.transactions.KafkaTransactions.class.getName());
    static final DotName KAFKA_REQUEST_REPLY_EMITTER = DotName.createSimple(io.smallrye.reactive.messaging.kafka.reply.KafkaRequestReply.class.getName());

    static final DotName TARGETED = DotName.createSimple(io.smallrye.reactive.messaging.Targeted.class.getName());
    static final DotName TARGETED_MESSAGES = DotName.createSimple(io.smallrye.reactive.messaging.TargetedMessages.class.getName());

    static final DotName MESSAGE = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Message.class.getName());

    static final DotName GENERIC_PAYLOAD = DotName.createSimple(io.smallrye.reactive.messaging.GenericPayload.class.getName());

    static final DotName KAFKA_RECORD = DotName.createSimple(io.smallrye.reactive.messaging.kafka.KafkaRecord.class.getName());
    static final DotName RECORD = DotName.createSimple(io.smallrye.reactive.messaging.kafka.Record.class.getName());
    static final DotName CONSUMER_RECORD = DotName.createSimple(org.apache.kafka.clients.consumer.ConsumerRecord.class.getName());
    static final DotName PRODUCER_RECORD = DotName.createSimple(org.apache.kafka.clients.producer.ProducerRecord.class.getName());

    static final DotName COMPLETION_STAGE = DotName.createSimple(java.util.concurrent.CompletionStage.class.getName());
    static final DotName UNI = DotName.createSimple(io.smallrye.mutiny.Uni.class.getName());

    static final DotName SUBSCRIBER = DotName.createSimple(org.reactivestreams.Subscriber.class.getName());
    static final DotName SUBSCRIBER_BUILDER = DotName.createSimple(org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder.class.getName());
    static final DotName PUBLISHER = DotName.createSimple(org.reactivestreams.Publisher.class.getName());
    static final DotName FLOW_PUBLISHER = DotName.createSimple(java.util.concurrent.Flow.Publisher.class.getName());
    static final DotName PUBLISHER_BUILDER = DotName.createSimple(org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder.class.getName());
    static final DotName PROCESSOR = DotName.createSimple(org.reactivestreams.Processor.class.getName());
    static final DotName PROCESSOR_BUILDER = DotName.createSimple(org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder.class.getName());
    static final DotName MULTI = DotName.createSimple(io.smallrye.mutiny.Multi.class.getName());
    static final DotName MULTI_SPLITTER = DotName.createSimple(io.smallrye.mutiny.operators.multi.split.MultiSplitter.class.getName());

    static final DotName AVRO_GENERATED = DotName.createSimple("org.apache.avro.specific.AvroGenerated");
    static final DotName AVRO_GENERIC_RECORD = DotName.createSimple("org.apache.avro.generic.GenericRecord");
    static final DotName KAFKA_SERIALIZER = DotName.createSimple(org.apache.kafka.common.serialization.Serializer.class.getName());
    static final DotName KAFKA_DESERIALIZER = DotName.createSimple(org.apache.kafka.common.serialization.Deserializer.class.getName());
    static final DotName OBJECT_MAPPER_DESERIALIZER = DotName.createSimple(io.quarkus.kafka.client.serialization.ObjectMapperDeserializer.class.getName());
    static final DotName OBJECT_MAPPER_SERIALIZER = DotName.createSimple(io.quarkus.kafka.client.serialization.ObjectMapperSerializer.class.getName());
    static final DotName JSONB_DESERIALIZER = DotName.createSimple(io.quarkus.kafka.client.serialization.JsonbDeserializer.class.getName());
    static final DotName JSONB_SERIALIZER = DotName.createSimple(io.quarkus.kafka.client.serialization.JsonbSerializer.class.getName());

    static final DotName LIST = DotName.createSimple(java.util.List.class.getName());
    static final DotName KAFKA_BATCH_RECORD = DotName.createSimple(io.smallrye.reactive.messaging.kafka.KafkaRecordBatch.class.getName());
    static final DotName CONSUMER_RECORDS = DotName.createSimple(org.apache.kafka.clients.consumer.ConsumerRecords.class.getName());
    // @formatter:on
}
