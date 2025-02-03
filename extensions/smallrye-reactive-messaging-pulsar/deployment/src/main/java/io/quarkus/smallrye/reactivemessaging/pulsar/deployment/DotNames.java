package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

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
    static final DotName PULSAR_EMITTER = DotName.createSimple(io.smallrye.reactive.messaging.pulsar.transactions.PulsarTransactions.class.getName());

    static final DotName TARGETED = DotName.createSimple(io.smallrye.reactive.messaging.Targeted.class.getName());
    static final DotName TARGETED_MESSAGES = DotName.createSimple(io.smallrye.reactive.messaging.TargetedMessages.class.getName());

    static final DotName MESSAGE = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Message.class.getName());

    static final DotName GENERIC_PAYLOAD = DotName.createSimple(io.smallrye.reactive.messaging.GenericPayload.class.getName());
    static final DotName PULSAR_MESSAGE = DotName.createSimple(io.smallrye.reactive.messaging.pulsar.PulsarMessage.class.getName());
    static final DotName PULSAR_BATCH_MESSAGE = DotName.createSimple(io.smallrye.reactive.messaging.pulsar.PulsarBatchMessage.class.getName());
    static final DotName PULSAR_API_MESSAGE = DotName.createSimple(org.apache.pulsar.client.api.Message.class.getName());
    static final DotName PULSAR_API_MESSAGES = DotName.createSimple(org.apache.pulsar.client.api.Messages.class.getName());
    static final DotName OUTGOING_MESSAGE = DotName.createSimple(io.smallrye.reactive.messaging.pulsar.OutgoingMessage.class.getName());

    static final DotName COMPLETION_STAGE = DotName.createSimple(java.util.concurrent.CompletionStage.class.getName());
    static final DotName UNI = DotName.createSimple(io.smallrye.mutiny.Uni.class.getName());

    static final DotName SUBSCRIBER = DotName.createSimple(org.reactivestreams.Subscriber.class.getName());
    static final DotName SUBSCRIBER_BUILDER = DotName.createSimple(org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder.class.getName());
    static final DotName PUBLISHER = DotName.createSimple(org.reactivestreams.Publisher.class.getName());
    static final DotName PUBLISHER_BUILDER = DotName.createSimple(org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder.class.getName());
    static final DotName PROCESSOR = DotName.createSimple(org.reactivestreams.Processor.class.getName());
    static final DotName PROCESSOR_BUILDER = DotName.createSimple(org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder.class.getName());
    static final DotName FLOW_PUBLISHER = DotName.createSimple(java.util.concurrent.Flow.Publisher.class.getName());
    static final DotName MULTI = DotName.createSimple(io.smallrye.mutiny.Multi.class.getName());
    static final DotName MULTI_SPLITTER = DotName.createSimple(io.smallrye.mutiny.operators.multi.split.MultiSplitter.class.getName());

    static final DotName PULSAR_GENERIC_RECORD = DotName.createSimple(org.apache.pulsar.client.api.schema.GenericRecord.class.getName());

    static final DotName AVRO_GENERATED = DotName.createSimple("org.apache.avro.specific.AvroGenerated");
    static final DotName AVRO_GENERIC_RECORD = DotName.createSimple("org.apache.avro.generic.GenericRecord");
    static final DotName PROTOBUF_GENERATED = DotName.createSimple("com.google.protobuf.GeneratedMessageV3");
    static final DotName PULSAR_SCHEMA = DotName.createSimple(org.apache.pulsar.client.api.Schema.class.getName());
    static final DotName PULSAR_AUTHENTICATION = DotName.createSimple(org.apache.pulsar.client.api.Authentication.class.getName());

    static final DotName LIST = DotName.createSimple(java.util.List.class.getName());

    static final DotName VERTX_BUFFER = DotName.createSimple(io.vertx.core.buffer.Buffer.class.getName());
    static final DotName VERTX_JSON_ARRAY = DotName.createSimple(io.vertx.core.json.JsonArray.class.getName());
    static final DotName VERTX_JSON_OBJECT = DotName.createSimple(io.vertx.core.json.JsonObject.class.getName());
    static final DotName BYTE_BUFFER = DotName.createSimple(java.nio.ByteBuffer.class.getName());

    static final DotName PRODUCES = DotName.createSimple(jakarta.enterprise.inject.Produces.class.getName());
    static final DotName IDENTIFIER = DotName.createSimple(io.smallrye.common.annotation.Identifier.class.getName());
    // @formatter:on
}
