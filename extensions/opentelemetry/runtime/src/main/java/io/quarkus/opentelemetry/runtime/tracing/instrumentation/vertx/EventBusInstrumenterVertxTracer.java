package io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx;

import static io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation.PUBLISH;
import static io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation.RECEIVE;
import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.OpenTelemetryVertxTracer.SpanOperation;
import io.quarkus.vertx.runtime.VertxEventBusConsumerRecorder;
import io.vertx.core.Context;
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

    /**
     * Overrides the default {@code sendResponse()} to support deferred scope closing
     * for blocking EventBus consumer handlers with fire-and-forget messages.
     * <p>
     * When a blocking handler sets {@link VertxEventBusConsumerRecorder#DEFER_SEND_RESPONSE_KEY}
     * on the Vert.x context, this method stores the scope closing and span ending logic as a
     * {@link Runnable} under {@link VertxEventBusConsumerRecorder#DEFERRED_TRACE_CLEANUP_KEY}
     * instead of executing it immediately.
     * This prevents the OTel context from being removed from Vert.x locals before
     * the worker thread runs the handler.
     *
     * @see <a href="https://github.com/eclipse-vertx/vert.x/issues/6021">eclipse-vertx/vert.x#6021</a>
     */
    @SuppressWarnings("unchecked")
    @Override
    public <R> void sendResponse(
            final Context context,
            final R response,
            final SpanOperation spanOperation,
            final Throwable failure,
            final TagExtractor<R> tagExtractor) {

        if (spanOperation == null) {
            return;
        }

        Scope scope = spanOperation.getScope();
        if (scope == null) {
            return;
        }

        Object request = spanOperation.getRequest();
        Instrumenter<Message, Message> instrumenter = getSendResponseInstrumenter();

        if (failure != null && response == null) {
            if (spanOperation.tryEndSpan()) {
                instrumenter.end(spanOperation.getSpanContext(), (Message) request, (Message) response, failure);
            }
        } else {
            Boolean defer = context.getLocal(VertxEventBusConsumerRecorder.DEFER_SEND_RESPONSE_KEY);
            if (Boolean.TRUE.equals(defer)) {
                context.removeLocal(VertxEventBusConsumerRecorder.DEFER_SEND_RESPONSE_KEY);
                context.putLocal(VertxEventBusConsumerRecorder.DEFERRED_TRACE_CLEANUP_KEY, (Runnable) () -> {
                    try (scope) {
                        if (spanOperation.tryEndSpan()) {
                            instrumenter.end(spanOperation.getSpanContext(), (Message) request, (Message) response,
                                    failure);
                        }
                    }
                });
                return;
            }
            try (scope) {
                if (spanOperation.tryEndSpan()) {
                    instrumenter.end(spanOperation.getSpanContext(), (Message) request, (Message) response, failure);
                }
            }
        }
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
