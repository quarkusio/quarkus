package io.quarkus.mongodb.tracing;

import org.jboss.logging.Logger;

import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;

import io.opentracing.contrib.mongo.common.TracingCommandListener;
import io.opentracing.util.GlobalTracer;

/**
 * Command Listener for Mongo client delegated to {@link TracingCommandListener}.
 *
 */
public class MongoTracingCommandListener implements CommandListener {

    private static final Logger LOGGER = Logger.getLogger(MongoTracingCommandListener.class);

    private final TracingCommandListener delegate;

    public MongoTracingCommandListener() {
        this.delegate = new TracingCommandListener.Builder(GlobalTracer.get()).build();
        LOGGER.debug("TracingCommandListener Delegate created");
    }

    @Override
    public void commandStarted(CommandStartedEvent event) {
        LOGGER.trace("commandStarted event " + event.getCommandName());
        delegate.commandStarted(event);
    }

    @Override
    public void commandFailed(CommandFailedEvent event) {
        LOGGER.trace("commandFailed event " + event.getCommandName());
        delegate.commandFailed(event);
    }

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        LOGGER.trace("commandSucceeded event " + event.getCommandName());
        delegate.commandSucceeded(event);
    }

}
