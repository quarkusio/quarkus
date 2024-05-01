package io.quarkus.funqy.lambda.event;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

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
import io.quarkus.funqy.lambda.model.FunqyMethod;
import io.quarkus.funqy.lambda.model.cloudevents.CloudEventV1;
import io.quarkus.funqy.lambda.model.kinesis.PipesKinesisEvent;
import io.quarkus.funqy.runtime.FunqyServerResponse;
import io.smallrye.mutiny.Uni;

public class EventProcessor {

    private static final Logger log = Logger.getLogger(EventProcessor.class);

    private final EventDeserializer eventDeserializer;
    private final ObjectMapper objectMapper;
    private final ObjectReader reader;
    private final FunqyMethod funqyMethod;
    private final Map<Class<?>, EventHandler<?, ?, ?>> eventHandlers;

    private FunqyAmazonConfig amazonConfig;

    public EventProcessor(ObjectMapper objectMapper, EventDeserializer eventDeserializer,
            FunqyMethod funqyMethod,
            FunqyAmazonBuildTimeConfig buildTimeConfig) {
        this.objectMapper = objectMapper;
        this.eventDeserializer = eventDeserializer;
        this.reader = objectMapper.readerFor(Object.class);
        this.funqyMethod = funqyMethod;

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

    public void init(FunqyAmazonConfig amazonConfig) {
        // This is a bit strange. We have some static values, which are initialized at some point in time.
        // See FunqyLambdaBindingRecorder. Furthermore, we need to pass through the runtime config.
        eventDeserializer.setFunqyMethodObjReader(funqyMethod.getReader().orElse(null));
        eventDeserializer.setObjectMapper(objectMapper);
        this.amazonConfig = amazonConfig;
    }

    public void handle(InputStream inputStream, OutputStream outputStream,
            Function<Object, FunqyServerResponse> dispatcher, Context context) throws IOException {
        // event might be null
        Object event = reader.readValue(inputStream);

        EventHandler<?, ?, ?> handler = getHandler(event);
        if (handler != null) {
            EventErrorHandler eventErrorHandler = new EventErrorHandler();

            Object value = handleEvent(handler, event, eventErrorHandler, dispatcher)
                    .await().indefinitely();

            if (value != null) {
                objectMapper.writeValue(outputStream, value);
            }
        } else {
            // Unknown event type. We do what Funqy normally did in the past.
            FunqyServerResponse response = dispatcher.apply(event);

            Object value = response.getOutput().await().indefinitely();
            writeOutput(outputStream, value);
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
        List<? extends Uni<?>> unis = handler.streamEvent((E) event, amazonConfig)
                .map(msg -> handleMessage(handler, eventErrorHandler, dispatcher, msg)).toList();

        return Uni.combine().all().unis(unis)
                .collectFailures().discardItems()
                .onFailure().recoverWithNull()
                .replaceWith(() -> handler.createResponse(eventErrorHandler.getFailures(), amazonConfig));
    }

    private <E, M, R> Uni<?> handleMessage(final EventHandler<E, M, R> handler, final EventErrorHandler eventErrorHandler,
            final Function<Object, FunqyServerResponse> dispatcher, final M msg) {
        try {
            final boolean isSuitableType = funqyMethod.getInputType().map(type -> type.hasRawClass(handler.getMessageClass()))
                    .orElse(false);

            Object input;
            if (isSuitableType) {
                input = msg;
            } else {
                input = readMessageBody(handler.getBody(msg, amazonConfig));
            }

            FunqyServerResponse response = dispatcher.apply(input);

            return eventErrorHandler.collectFailures(response.getOutput(), handler.getIdentifier(msg, amazonConfig));
        } catch (Throwable e) {
            log.errorv(e, """
                    Event could not be handled. This might happen, when the lambda is used with a not supported \
                    trigger. If this happens you should disable the advanced event handling and handle the event \
                    manually.
                    """);
            return eventErrorHandler.collectFailures(Uni.createFrom().failure(e),
                    handler.getIdentifier(msg, amazonConfig));
        }
    }

    private Object readMessageBody(Supplier<InputStream> is) throws IOException {
        if (funqyMethod.getReader().isEmpty()) {
            return null;
        }
        return funqyMethod.getReader().get().readValue(is.get());
    }

    private void writeOutput(final OutputStream outputStream, final Object value) throws IOException {
        if (funqyMethod.getWriter().isPresent() && value != null) {
            funqyMethod.getWriter().get().writeValue(outputStream, value);
        }
    }
}
