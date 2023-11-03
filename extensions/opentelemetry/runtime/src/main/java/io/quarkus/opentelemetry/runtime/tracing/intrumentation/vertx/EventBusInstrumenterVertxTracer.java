package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import static io.opentelemetry.instrumentation.api.instrumenter.messaging.MessageOperation.PUBLISH;
import static io.opentelemetry.instrumentation.api.instrumenter.messaging.MessageOperation.RECEIVE;
import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.messaging.MessagingAttributesGetter;
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
        InstrumenterBuilder<Message, Message> serverBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME, MessagingSpanNameExtractor.create(EventBusAttributesGetter.INSTANCE, RECEIVE));

        return serverBuilder
                .addAttributesExtractor(MessagingAttributesExtractor.create(EventBusAttributesGetter.INSTANCE, RECEIVE))
                .buildConsumerInstrumenter(new TextMapGetter<>() {
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
        InstrumenterBuilder<Message, Message> serverBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME, MessagingSpanNameExtractor.create(EventBusAttributesGetter.INSTANCE, PUBLISH));

        return serverBuilder
                .addAttributesExtractor(MessagingAttributesExtractor.create(EventBusAttributesGetter.INSTANCE, PUBLISH))
                .buildProducerInstrumenter((message, key, value) -> {
                    if (message != null) {
                        message.headers().set(key, value);
                    }
                });
    }

    private enum EventBusAttributesGetter implements MessagingAttributesGetter<Message, Message> {
        INSTANCE;

        @Override
        public String getSystem(final Message message) {
            return "vert.x";
        }

        @Override
        public String getDestination(final Message message) {
            return message.address();
        }

        @Override
        public boolean isTemporaryDestination(final Message message) {
            return false;
        }

        @Override
        public String getConversationId(final Message message) {
            return message.replyAddress();
        }

        @Override
        public Long getMessagePayloadSize(final Message message) {
            return null;
        }

        @Override
        public Long getMessagePayloadCompressedSize(final Message message) {
            return null;
        }

        @Override
        public String getMessageId(final Message message, final Message message2) {
            return null;
        }
    }
}
