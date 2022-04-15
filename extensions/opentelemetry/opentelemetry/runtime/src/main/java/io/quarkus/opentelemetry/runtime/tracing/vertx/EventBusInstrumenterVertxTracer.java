package io.quarkus.opentelemetry.runtime.tracing.vertx;

import static io.opentelemetry.instrumentation.api.instrumenter.messaging.MessageOperation.RECEIVE;
import static io.opentelemetry.instrumentation.api.instrumenter.messaging.MessageOperation.SEND;
import static io.quarkus.opentelemetry.runtime.OpenTelemetryConfig.INSTRUMENTATION_NAME;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.instrumenter.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.messaging.MessagingSpanNameExtractor;
import io.vertx.core.eventbus.Message;
import io.vertx.core.spi.tracing.TagExtractor;

@SuppressWarnings("rawtypes")
public class EventBusInstrumenterVertxTracer implements InstrumenterVertxTracer<Message, Message> {
    private final Instrumenter<Message, Message> consumerInstrumenter;
    private final Instrumenter<Message, Message> producerInstrumenter;

    public EventBusInstrumenterVertxTracer(final OpenTelemetry openTelemetry) {
        this.consumerInstrumenter = getConsumerInstrumenter(openTelemetry);
        this.producerInstrumenter = getProducerInstrumenter(openTelemetry);
    }

    @Override
    public <R> boolean canHandle(final R request, final TagExtractor<R> tagExtractor) {
        return request instanceof Message;
    }

    @Override
    public Instrumenter<Message, Message> getReceiveRequestInstrumenter() {
        return consumerInstrumenter;
    }

    @Override
    public Instrumenter<Message, Message> getSendResponseInstrumenter() {
        return consumerInstrumenter;
    }

    @Override
    public Instrumenter<Message, Message> getSendRequestInstrumenter() {
        return producerInstrumenter;
    }

    @Override
    public Instrumenter<Message, Message> getReceiveResponseInstrumenter() {
        return producerInstrumenter;
    }

    private static Instrumenter<Message, Message> getConsumerInstrumenter(final OpenTelemetry openTelemetry) {
        EventBusAttributesExtractor eventBusAttributesExtractor = new EventBusAttributesExtractor(RECEIVE);

        InstrumenterBuilder<Message, Message> serverBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME, MessagingSpanNameExtractor.create(eventBusAttributesExtractor));

        return serverBuilder
                .addAttributesExtractor(eventBusAttributesExtractor)
                .newConsumerInstrumenter(new TextMapGetter<>() {
                    @Override
                    public Iterable<String> keys(final Message message) {
                        return message.headers().names();
                    }

                    @Override
                    public String get(final Message message, final String key) {
                        if (message == null) {
                            return null;
                        }
                        return message.headers().get(key);
                    }
                });
    }

    private static Instrumenter<Message, Message> getProducerInstrumenter(final OpenTelemetry openTelemetry) {
        EventBusAttributesExtractor eventBusAttributesExtractor = new EventBusAttributesExtractor(SEND);

        InstrumenterBuilder<Message, Message> serverBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME, MessagingSpanNameExtractor.create(eventBusAttributesExtractor));

        return serverBuilder
                .addAttributesExtractor(eventBusAttributesExtractor)
                .newProducerInstrumenter((message, key, value) -> {
                    if (message != null) {
                        message.headers().set(key, value);
                    }
                });
    }

    private static class EventBusAttributesExtractor extends MessagingAttributesExtractor<Message, Message> {
        private final MessageOperation operation;

        public EventBusAttributesExtractor(final MessageOperation operation) {
            this.operation = operation;
        }

        @Override
        public MessageOperation operation() {
            return operation;
        }

        @Override
        protected String system(final Message message) {
            return "vert.x";
        }

        @Override
        protected String destinationKind(final Message message) {
            return message.isSend() ? "queue" : "topic";
        }

        @Override
        protected String destination(final Message message) {
            return message.address();
        }

        @Override
        protected boolean temporaryDestination(final Message message) {
            return false;
        }

        @Override
        protected String protocol(final Message message) {
            return null;
        }

        @Override
        protected String protocolVersion(final Message message) {
            return "4.0";
        }

        @Override
        protected String url(final Message message) {
            return null;
        }

        @Override
        protected String conversationId(final Message message) {
            return message.replyAddress();
        }

        @Override
        protected Long messagePayloadSize(final Message message) {
            return null;
        }

        @Override
        protected Long messagePayloadCompressedSize(final Message message) {
            return null;
        }

        @Override
        protected String messageId(final Message message, final Message message2) {
            return null;
        }
    }
}
