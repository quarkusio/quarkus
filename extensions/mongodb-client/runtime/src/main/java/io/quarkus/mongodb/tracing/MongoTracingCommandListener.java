package io.quarkus.mongodb.tracing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.mongodb.event.*;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;

public class MongoTracingCommandListener implements CommandListener {
    private static final org.jboss.logging.Logger LOGGER = Logger.getLogger(MongoTracingCommandListener.class);
    private static final String KEY = "mongodb.command";
    private final Map<Integer, ContextEvent> requestMap;
    private final Instrumenter<CommandStartedEvent, Void> instrumenter;

    private record ContextEvent(Context context, CommandStartedEvent commandEvent) {
    }

    @Inject
    public MongoTracingCommandListener(OpenTelemetry openTelemetry) {
        requestMap = new ConcurrentHashMap<>();
        SpanNameExtractor<CommandStartedEvent> spanNameExtractor = CommandEvent::getCommandName;
        instrumenter = Instrumenter.<CommandStartedEvent, Void> builder(
                openTelemetry, "quarkus-mongodb-client", spanNameExtractor)
                .addAttributesExtractor(new CommandEventAttrExtractor())
                .buildInstrumenter(SpanKindExtractor.alwaysClient());
        LOGGER.debugf("MongoTracingCommandListener created");
    }

    @Override
    public void commandStarted(CommandStartedEvent event) {
        LOGGER.tracef("commandStarted event %s", event.getCommandName());

        Context parentContext = Context.current();
        if (instrumenter.shouldStart(parentContext, event)) {
            Context context = instrumenter.start(parentContext, event);
            requestMap.put(event.getRequestId(), new ContextEvent(context, event));
        }
    }

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        LOGGER.tracef("commandSucceeded event %s", event.getCommandName());
        ContextEvent contextEvent = requestMap.remove(event.getRequestId());
        if (contextEvent != null) {
            instrumenter.end(contextEvent.context(), contextEvent.commandEvent(), null, null);
        }
    }

    @Override
    public void commandFailed(CommandFailedEvent event) {
        LOGGER.tracef("commandFailed event %s", event.getCommandName());
        ContextEvent contextEvent = requestMap.remove(event.getRequestId());
        if (contextEvent != null) {
            instrumenter.end(
                    contextEvent.context(),
                    contextEvent.commandEvent(),
                    null,
                    event.getThrowable());
        }
    }

    private static class CommandEventAttrExtractor implements AttributesExtractor<CommandStartedEvent, Void> {
        @Override
        public void onStart(AttributesBuilder attributesBuilder,
                Context context,
                CommandStartedEvent commandStartedEvent) {
            attributesBuilder.put(KEY, commandStartedEvent.getCommand().toJson());
        }

        @Override
        public void onEnd(AttributesBuilder attributesBuilder,
                Context context,
                CommandStartedEvent commandStartedEvent,
                @Nullable Void unused,
                @Nullable Throwable throwable) {

        }
    }
}
