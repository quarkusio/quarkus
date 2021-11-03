package io.quarkus.reactivemessaging.http.runtime;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.config.HttpStreamConfig;
import io.quarkus.reactivemessaging.http.runtime.config.ReactiveHttpConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * a bean that handles incoming http requests
 */
@Singleton
public class ReactiveHttpHandlerBean extends ReactiveHandlerBeanBase<HttpStreamConfig, HttpMessage<?>> {

    private static final Logger log = Logger.getLogger(ReactiveHttpHandlerBean.class);

    @Inject
    ReactiveHttpConfig config;

    Multi<HttpMessage<?>> getProcessor(String path, HttpMethod method) {
        return processors.get(key(path, method)).getProcessor();
    }

    @Override
    protected Collection<HttpStreamConfig> configs() {
        return config.getHttpConfigs();
    }

    @Override
    protected String key(HttpStreamConfig streamConfig) {
        return key(streamConfig.path, streamConfig.method);
    }

    @Override
    protected String key(RoutingContext context) {
        return key(context.normalizedPath(), context.request().method());
    }

    @Override
    protected String description(HttpStreamConfig streamConfig) {
        return String.format("path: %s, method %s", streamConfig.path, streamConfig.method);
    }

    @Override
    protected void handleRequest(RoutingContext event, MultiEmitter<? super HttpMessage<?>> emitter,
            StrictQueueSizeGuard guard, String path) {
        if (emitter == null) {
            onUnexpectedError(event, null,
                    "No consumer subscribed for messages sent to Reactive Messaging HTTP endpoint on path: " + path);
        } else if (guard.prepareToEmit()) {
            try {
                HttpMessage<Buffer> message = new HttpMessage<>(event.getBody(), new IncomingHttpMetadata(event.request()),
                        () -> event.response().setStatusCode(202).end(),
                        error -> onUnexpectedError(event, error, "Failed to process message."));
                emitter.emit(message);
            } catch (Exception any) {
                guard.dequeue();
                onUnexpectedError(event, any, "Emitting message failed");
            }
        } else {
            event.response().setStatusCode(503).end();
        }
    }

    private void onUnexpectedError(RoutingContext event, Throwable error, String message) {
        event.response().setStatusCode(500).end("Unexpected error while processing the message");
        log.error(message, error);
    }

    private String key(String path, HttpMethod method) {
        return String.format("%s:%s", path, method);
    }
}
