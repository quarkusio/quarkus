package io.quarkus.funqy.lambda.event;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectReader;

import io.quarkus.funqy.lambda.FunqyResponseImpl;
import io.quarkus.funqy.lambda.config.FunqyAmazonBuildTimeConfig;
import io.quarkus.funqy.lambda.config.FunqyAmazonConfig;
import io.quarkus.funqy.lambda.event.cloudevents.CloudEventsHandler;
import io.quarkus.funqy.lambda.event.dynamodb.DynamoDbEventHandler;
import io.quarkus.funqy.lambda.event.dynamodb.PipesDynamoDbEventHandler;
import io.quarkus.funqy.lambda.event.kinesis.KinesisEventHandler;
import io.quarkus.funqy.lambda.event.kinesis.PipesKinesisEventHandler;
import io.quarkus.funqy.lambda.event.sns.SnsEventHandler;
import io.quarkus.funqy.lambda.event.sqs.PipesSqsEventHandler;
import io.quarkus.funqy.lambda.event.sqs.SqsEventHandler;
import io.quarkus.funqy.lambda.model.cloudevents.CloudEventV1;
import io.quarkus.funqy.lambda.model.kinesis.PipesKinesisEvent;
import io.quarkus.funqy.runtime.FunqyServerResponse;
import io.smallrye.mutiny.Uni;

public class EventProcessor {

    private static final Logger log = Logger.getLogger(EventProcessor.class);

    private final ObjectReader objectReader;
    private final FunqyAmazonConfig config;
    private final Map<Class<?>, EventHandler<?, ?, ?>> eventHandlers;

    public EventProcessor(final ObjectReader objectReader,
            final FunqyAmazonBuildTimeConfig buildTimeConfig, final FunqyAmazonConfig config) {
        this.objectReader = objectReader;
        this.config = config;

        this.eventHandlers = new HashMap<>();
        if (buildTimeConfig.advancedEventHandling().enabled()) {
            this.eventHandlers.put(SQSEvent.class, new SqsEventHandler());
            this.eventHandlers.put(SQSEvent.SQSMessage.class, new PipesSqsEventHandler());
            this.eventHandlers.put(SNSEvent.class, new SnsEventHandler());
            this.eventHandlers.put(KinesisEvent.class, new KinesisEventHandler());
            this.eventHandlers.put(PipesKinesisEvent.class, new PipesKinesisEventHandler());
            this.eventHandlers.put(DynamodbEvent.class, new DynamoDbEventHandler());
            this.eventHandlers.put(DynamodbEvent.DynamodbStreamRecord.class, new PipesDynamoDbEventHandler());
            this.eventHandlers.put(CloudEventV1.class, new CloudEventsHandler());
        }
    }

    public FunqyServerResponse handle(Object event,
            Function<Object, FunqyServerResponse> dispatcher, Context context) throws IOException {

        EventHandler<?, ?, ?> handler = getHandler(event);

        if (handler != null) {
            EventErrorHandler eventErrorHandler = new EventErrorHandler();

            FunqyResponseImpl funqyResponse = new FunqyResponseImpl();
            funqyResponse.setOutput(handleEvent(handler, event, eventErrorHandler, dispatcher));
            return funqyResponse;

        } else {
            // Unknown event type. We do what Funqy normally did in the past.
            return dispatcher.apply(event);
        }
    }

    private EventHandler<?, ?, ?> getHandler(Object event) {
        if (event == null) {
            return null;
        }
        if (event instanceof List<?> list && !list.isEmpty()) {
            // we need some special handling for lists
            return eventHandlers.get(list.get(0).getClass());
        }

        return eventHandlers.get(event.getClass());
    }

    @SuppressWarnings("unchecked")
    private <E, M, R> Uni<?> handleEvent(EventHandler<E, M, R> handler, Object event,
            EventErrorHandler eventErrorHandler, Function<Object, FunqyServerResponse> dispatcher) {

        // We collect all messages in a list first, so that we can execute them in parallel.
        List<? extends Uni<?>> unis = handler.streamEvent((E) event, config)
                .map(msg -> handleMessage(handler, eventErrorHandler, dispatcher, msg)).toList();

        log.debugv("Received {0} messages in a batch.", unis.size());

        return Uni.combine().all().unis(unis)
                .collectFailures().discardItems()
                .onFailure().invoke(err -> log.errorv(err, "An exception occurred during message handling."))
                .onFailure().recoverWithNull()
                .replaceWith(() -> {
                    log.debugv("Detected {0} errors during message handling.", unis.size());
                    return handler.createResponse(eventErrorHandler.getFailures(), config);
                });
    }

    private <E, M, R> Uni<?> handleMessage(final EventHandler<E, M, R> handler, final EventErrorHandler eventErrorHandler,
            final Function<Object, FunqyServerResponse> dispatcher, final M msg) {
        try {
            // We check if the funqy method already uses the event model
            final boolean isUsingEventModel = Optional.ofNullable(objectReader).map(ObjectReader::getValueType)
                    .map(type -> type.hasRawClass(handler.getMessageClass()))
                    .orElse(false);

            Object input;
            if (isUsingEventModel) {
                // If the funqy method is using the event model we do not need to deserialize the content
                log.debug("Funqy method is using the event model. No further deserialization necessary.");
                input = msg;
            } else {
                // The funqy method uses a custom model. We need to ask the handle to provide the content and then
                // we deserialize it.
                log.debug("Funqy method is using a custom model. Try to deserialize message.");
                input = readMessageBody(handler.getBody(msg, config));
            }

            FunqyServerResponse response = dispatcher.apply(input);

            return eventErrorHandler.collectFailures(response.getOutput(), handler.getIdentifier(msg, config));
        } catch (Throwable e) {
            log.errorv(e, """
                    Event could not be handled. This can have multiple reasons:
                    1. Message body could not be deserialized
                    2. Using a not supported AWS event
                    """);
            return eventErrorHandler.collectFailures(Uni.createFrom().failure(e),
                    handler.getIdentifier(msg, config));
        }
    }

    private Object readMessageBody(Supplier<InputStream> is) throws IOException {
        if (objectReader == null) {
            return null;
        }
        return objectReader.readValue(is.get());
    }
}
