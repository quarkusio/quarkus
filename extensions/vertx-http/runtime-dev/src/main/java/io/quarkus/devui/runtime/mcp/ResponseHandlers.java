package io.quarkus.devui.runtime.mcp;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class ResponseHandlers {

    private static final Logger LOG = Logger.getLogger(ResponseHandlers.class);

    // request id -> response handler
    private final ConcurrentMap<Long, ResponseHandler> handlers;

    private final AtomicLong idGenerator;

    public ResponseHandlers() {
        this.handlers = new ConcurrentHashMap<>();
        this.idGenerator = new AtomicLong();
    }

    boolean hasHandler(long id) {
        return handlers.containsKey(id);
    }

    Long nextId() {
        return idGenerator.incrementAndGet();
    }

    Long newRequest(Consumer<JsonObject> responseConsumer) {
        Long nextId = nextId();
        handlers.put(nextId, new ResponseHandler(Instant.now(), responseConsumer));
        return nextId;
    }

    boolean remove(long id) {
        if (handlers.remove(id) != null) {
            LOG.debugf("Removed response handler for %s", id);
            return true;
        }
        return false;
    }

    Future<Void> handleResponse(Object id, JsonObject message) {
        if (id == null) {
            LOG.debugf("Discard client response with no id: %s", message);
        } else {
            ResponseHandler handler = handlers.remove(coerceResponseId(id));
            if (handler == null) {
                // Discard all responses for which a handler is not found
                // including pong responses
                LOG.debugf("Handler not found - discard client response with id %s", id);
            } else {
                try {
                    handler.operation().accept(message);
                } catch (Throwable e) {
                    LOG.errorf(e, "Unable to process the response with id %s", id);
                    return Future.failedFuture(e);
                }
            }
        }
        return Future.succeededFuture();
    }

    private Long coerceResponseId(Object id) {
        if (id instanceof Long longId) {
            return longId;
        } else if (id instanceof Integer intId) {
            return intId.longValue();
        } else if (id instanceof String strId) {
            return Long.parseLong(strId);
        }
        throw new IllegalArgumentException("Unsupported response identifier: " + id);
    }

    private record ResponseHandler(Instant creationTime, Consumer<JsonObject> operation) {

        public ResponseHandler {
            if (creationTime == null) {
                throw new IllegalArgumentException("creationTime must not be null");
            }
            if (operation == null) {
                throw new IllegalArgumentException("operation must not be null");
            }
        }

    }

}
