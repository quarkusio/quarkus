package io.quarkus.smallrye.graphql.runtime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.JsonObject;

import org.jboss.logging.Logger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import graphql.ExecutionResult;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.smallrye.graphql.bootstrap.Config;
import io.smallrye.graphql.execution.ExecutionResponse;
import io.smallrye.graphql.execution.error.ExecutionErrorsService;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that does the execution of GraphQL Requests
 */
public class SmallRyeGraphQLSubscriptionHandler extends SmallRyeGraphQLAbstractHandler {
    private static final Logger log = Logger.getLogger(SmallRyeGraphQLSubscriptionHandler.class);
    private final ExecutionErrorsService executionErrorsService;
    private final Config config;
    private final ConcurrentHashMap<String, AtomicReference<Subscription>> subscriptionRefs = new ConcurrentHashMap<>();

    public SmallRyeGraphQLSubscriptionHandler(Config config, CurrentIdentityAssociation currentIdentityAssociation,
            CurrentVertxRequest currentVertxRequest) {
        super(currentIdentityAssociation, currentVertxRequest);
        this.config = config;
        this.executionErrorsService = new ExecutionErrorsService(config);
    }

    @Override
    protected void doHandle(final RoutingContext ctx) {
        if (ctx.request().headers().contains(HttpHeaders.UPGRADE, HttpHeaders.WEBSOCKET, true) && !ctx.request().isEnded()) {
            ctx.request().toWebSocket(new SmallRyeWebSocketHandler());
        } else {
            ctx.next();
        }
    }

    private class SmallRyeWebSocketHandler implements Handler<AsyncResult<ServerWebSocket>> {

        @Override
        public void handle(AsyncResult<ServerWebSocket> event) {
            if (event.succeeded()) {
                ServerWebSocket serverWebSocket = event.result();
                serverWebSocket.closeHandler(new CloseHandler(event.result().textHandlerID()));
                serverWebSocket.endHandler(new EndHandler(event.result().textHandlerID()));
                serverWebSocket.exceptionHandler(new ExceptionHandler(event.result().textHandlerID()));
                serverWebSocket.textMessageHandler(new TextMessageHandler(serverWebSocket));
            }
        }
    }

    private class CloseHandler implements Handler<Void> {
        String socketId;

        public CloseHandler(String socketId) {
            this.socketId = socketId;
        }

        @Override
        public void handle(Void e) {
            unsubscribe(socketId);
        }
    }

    private class EndHandler implements Handler<Void> {
        String socketId;

        public EndHandler(String socketId) {
            this.socketId = socketId;
        }

        @Override
        public void handle(Void e) {
            unsubscribe(socketId);
        }
    }

    private class ExceptionHandler implements Handler<Throwable> {
        String socketId;

        public ExceptionHandler(String socketId) {
            this.socketId = socketId;
        }

        @Override
        public void handle(Throwable e) {
            log.error(e.getMessage());
            unsubscribe(socketId);
        }
    }

    public void unsubscribe(String textHandlerId) {
        AtomicReference<Subscription> subscription = subscriptionRefs.get(textHandlerId);
        subscriptionRefs.remove(textHandlerId);

        if (subscription != null && subscription.get() != null) {
            Subscriptions.cancel(subscription);
            subscription.set(null);
        }
    }

    private class TextMessageHandler implements Handler<String> {
        private final SmallRyeGraphQLSubscriptionSubscriber smallRyeGraphQLSubscriptionSubscriber;

        TextMessageHandler(final ServerWebSocket serverWebSocket) {
            this.smallRyeGraphQLSubscriptionSubscriber = new SmallRyeGraphQLSubscriptionSubscriber(serverWebSocket);
        }

        @Override
        public void handle(String message) {
            JsonObject jsonInput = inputToJsonObject(message);
            ExecutionResponse executionResponse = getExecutionService()
                    .execute(jsonInput);

            ExecutionResult executionResult = executionResponse.getExecutionResult();

            if (executionResult != null) {
                // If there is error on the query, we can not start a subscription
                if (executionResult.getErrors() != null && !executionResult.getErrors().isEmpty()) {
                    smallRyeGraphQLSubscriptionSubscriber.onNext(executionResult);
                    smallRyeGraphQLSubscriptionSubscriber.closeWebSocket();
                } else {
                    Publisher<ExecutionResult> stream = executionResponse.getExecutionResult()
                            .getData();

                    if (stream != null) {
                        stream.subscribe(smallRyeGraphQLSubscriptionSubscriber);
                    }
                }
            }
        }
    }

    private class SmallRyeGraphQLSubscriptionSubscriber implements Subscriber<ExecutionResult> {
        private final ServerWebSocket serverWebSocket;
        private final String textHandlerId;

        public SmallRyeGraphQLSubscriptionSubscriber(ServerWebSocket serverWebSocket) {
            this.serverWebSocket = serverWebSocket;
            this.textHandlerId = serverWebSocket.textHandlerID();
        }

        @Override
        public void onSubscribe(Subscription s) {
            AtomicReference<Subscription> subRef = subscriptionRefs.get(serverWebSocket.textHandlerID());
            if (subRef == null) {
                subRef = new AtomicReference<>(s);
                subscriptionRefs.put(textHandlerId, subRef);
                s.request(1);
                return;
            }
            if (subRef.compareAndSet(null, s)) {
                s.request(1);
            } else {
                s.cancel();
            }
        }

        @Override
        public void onNext(ExecutionResult executionResult) {
            if (serverWebSocket != null && !serverWebSocket.isClosed()) {
                ExecutionResponse executionResponse = new ExecutionResponse(executionResult, config);
                serverWebSocket.writeTextMessage(executionResponse.getExecutionResultAsString());
                Subscription s = subscriptionRefs.get(textHandlerId).get();
                s.request(1);
            } else {
                // Connection to client is closed, but we still receive messages
                unsubscribe(textHandlerId);
            }
        }

        @Override
        public void onError(Throwable thrwbl) {
            log.error("Error in GraphQL Subscription Websocket", thrwbl);
            unsubscribe(serverWebSocket.textHandlerID());
            closeWebSocket();
        }

        @Override
        public void onComplete() {
            unsubscribe(serverWebSocket.textHandlerID());
            closeWebSocket();
        }

        public void closeWebSocket() {
            if (!serverWebSocket.isClosed()) {
                serverWebSocket.close();
            }
        }
    }
}