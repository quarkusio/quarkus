package io.quarkus.mongodb.tracing;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;

import org.bson.BsonDocument;
import org.jboss.logging.Logger;

import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.quarkus.mongodb.runtime.MongoRequestContext;

public class MongoTracingCommandListener implements CommandListener {
    private static final org.jboss.logging.Logger LOGGER = Logger.getLogger(MongoTracingCommandListener.class);
    private static final String KEY = "mongodb.command";
    private final Map<Integer, ContextEvent> requestMap;
    private final Instrumenter<MongoCommand, Void> instrumenter;

    private record MongoCommand(String name, BsonDocument command) {
    }

    private record ContextEvent(Context context, MongoCommand command) {
    }

    @Inject
    public MongoTracingCommandListener(OpenTelemetry openTelemetry) {
        requestMap = new ConcurrentHashMap<>();
        SpanNameExtractor<MongoCommand> spanNameExtractor = MongoCommand::name;
        instrumenter = Instrumenter.<MongoCommand, Void> builder(
                openTelemetry, "quarkus-mongodb-client", spanNameExtractor)
                .addAttributesExtractor(new CommandEventAttrExtractor())
                .buildInstrumenter(SpanKindExtractor.alwaysClient());
        LOGGER.debugf("MongoTracingCommandListener created");
    }

    @Override
    public void commandStarted(CommandStartedEvent event) {
        LOGGER.tracef("commandStarted event %s", event.getCommandName());

        Context parentContext = Optional.ofNullable(event.getRequestContext())
                .map(rc -> {
                    Context ctx = rc.get(MongoRequestContext.OTEL_CONTEXT_KEY);
                    rc.delete(MongoRequestContext.OTEL_CONTEXT_KEY);
                    return ctx;
                })
                .orElseGet(Context::current);
        var mongoCommand = new MongoCommand(event.getCommandName(), event.getCommand());
        if (instrumenter.shouldStart(parentContext, mongoCommand)) {
            Context context = instrumenter.start(parentContext, mongoCommand);
            requestMap.put(event.getRequestId(), new ContextEvent(context, mongoCommand));
        }
    }

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        LOGGER.tracef("commandSucceeded event %s", event.getCommandName());
        ContextEvent contextEvent = requestMap.remove(event.getRequestId());
        if (contextEvent != null) {
            instrumenter.end(contextEvent.context(), contextEvent.command(), null, null);
        }
    }

    @Override
    public void commandFailed(CommandFailedEvent event) {
        LOGGER.tracef("commandFailed event %s", event.getCommandName());
        ContextEvent contextEvent = requestMap.remove(event.getRequestId());
        if (contextEvent != null) {
            instrumenter.end(
                    contextEvent.context(),
                    contextEvent.command(),
                    null,
                    event.getThrowable());
        }
    }

    private static class CommandEventAttrExtractor implements AttributesExtractor<MongoCommand, Void> {

        @Override
        public void onStart(AttributesBuilder attributesBuilder, Context context, MongoCommand command) {
            attributesBuilder.put(KEY, command.command().toJson());
        }

        @Override
        public void onEnd(
                AttributesBuilder attributesBuilder,
                Context context,
                MongoCommand command,
                Void unused,
                Throwable throwable) {
        }
    }
}
