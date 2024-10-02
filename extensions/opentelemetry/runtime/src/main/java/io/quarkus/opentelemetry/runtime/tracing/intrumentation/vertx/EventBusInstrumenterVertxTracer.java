package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import static io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation.PUBLISH;
import static io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation.RECEIVE;
import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.vertx.core.eventbus.Message;
import io.vertx.core.spi.tracing.TagExtractor;

@SuppressWarnings("rawtypes")
public class EventBusInstrumenterVertxTracer implements InstrumenterVertxTracer<Message, Message> {
    private final Instrumenter<Message, Message> consumerInstrumenter;
    private final Instrumenter<Message, Message> producerInstrumenter;

    public EventBusInstrumenterVertxTracer(final OpenTelemetry openTelemetry, final OTelRuntimeConfig runtimeConfig) {
        this.consumerInstrumenter = getConsumerInstrumenter(openTelemetry, runtimeConfig);
        this.producerInstrumenter = getProducerInstrumenter(openTelemetry, runtimeConfig);
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

    private static Instrumenter<Message, Message> getConsumerInstrumenter(final OpenTelemetry openTelemetry,
            final OTelRuntimeConfig runtimeConfig) {
        InstrumenterBuilder<Message, Message> serverBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME, MessagingSpanNameExtractor.create(EventBusAttributesGetter.INSTANCE, RECEIVE));

        serverBuilder.setEnabled(!runtimeConfig.sdkDisabled());

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

    private static Instrumenter<Message, Message> getProducerInstrumenter(final OpenTelemetry openTelemetry,
            final OTelRuntimeConfig runtimeConfig) {
        InstrumenterBuilder<Message, Message> serverBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME, MessagingSpanNameExtractor.create(EventBusAttributesGetter.INSTANCE, PUBLISH));

        serverBuilder.setEnabled(!runtimeConfig.sdkDisabled());

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
        public String getDestinationTemplate(Message message) {
            return "";
        }

        @Override
        public boolean isTemporaryDestination(final Message message) {
            return false;
        }

        @Override
        public boolean isAnonymousDestination(Message message) {
            return false;
        }

        @Override
        public String getConversationId(final Message message) {
            return message.replyAddress();
        }

        @Override
        public Long getMessageBodySize(Message message) {
            return 0L;
        }

        @Override
        public Long getMessageEnvelopeSize(Message message) {
            return 0L;
        }

        @Override
        public String getMessageId(final Message message, final Message message2) {
            return null;
        }

        @Override
        public String getClientId(Message message) {
            return "";
        }

        @Override
        public Long getBatchMessageCount(Message message, Message message2) {
            return 0L;
        }
    }
}
