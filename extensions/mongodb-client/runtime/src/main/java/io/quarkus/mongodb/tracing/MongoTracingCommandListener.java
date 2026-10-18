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
import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.mongodb.runtime.MongoRequestContext;
import io.quarkus.mongodb.runtime.MongoTracingRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;

public class MongoTracingCommandListener implements CommandListener {
    private static final org.jboss.logging.Logger LOGGER = Logger.getLogger(MongoTracingCommandListener.class);
    private static final String KEY = "db.query.text";
    private final Map<Integer, ContextEvent> requestMap;
    private final Instrumenter<MongoCommand, Void> instrumenter;
    private final MongoCommandSanitizer sanitizer;
    private final MongoTracingRuntimeConfig config;

    private record MongoCommand(String name, BsonDocument command) {
    }

    private record ContextEvent(Context context, MongoCommand command) {
    }

    @Inject
    public MongoTracingCommandListener(
            OpenTelemetry openTelemetry,
            MongoConfig mongoConfig,
            OTelRuntimeConfig oTelRuntimeConfig) {

        this.config = mongoConfig.tracing();
        this.sanitizer = new MongoCommandSanitizer();
        this.requestMap = new ConcurrentHashMap<>();

        SpanNameExtractor<MongoCommand> spanNameExtractor = MongoCommand::name;
        instrumenter = Instrumenter.<MongoCommand, Void> builder(
                openTelemetry, "quarkus-mongodb-client", spanNameExtractor)
                .setEnabled(!oTelRuntimeConfig.sdkDisabled())
                .addAttributesExtractor(new CommandEventAttrExtractor(sanitizer, config))
                .buildInstrumenter(SpanKindExtractor.alwaysClient());
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

        private final MongoCommandSanitizer sanitizer;
        private final MongoTracingRuntimeConfig config;

        CommandEventAttrExtractor(MongoCommandSanitizer sanitizer, MongoTracingRuntimeConfig config) {
            this.sanitizer = sanitizer;
            this.config = config;
        }

        @Override
        public void onStart(AttributesBuilder attributesBuilder, Context context, MongoCommand command) {
            // Only add operation metadata as standard OTel attributes
            String collectionName = sanitizer.extractCollectionName(command.command());
            if (collectionName != null) {
                attributesBuilder.put("db.collection.name", collectionName);
            }

            switch (config.commandDetailLevel()) {
                case OFF:
                    break;
                case SANITIZED:
                    attributesBuilder.put(KEY, sanitizer.sanitizeCommand(command.command()));
                    break;
                case FULL:
                    attributesBuilder.put(KEY, command.command().toJson());
                    break;
            }
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
