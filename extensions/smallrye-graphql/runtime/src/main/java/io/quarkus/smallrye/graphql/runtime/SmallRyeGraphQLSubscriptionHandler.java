package io.quarkus.smallrye.graphql.runtime;

import java.util.concurrent.atomic.AtomicReference;

import javax.json.JsonObject;

import org.jboss.logging.Logger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphqlErrorBuilder;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.smallrye.graphql.bootstrap.Config;
import io.smallrye.graphql.execution.ExecutionResponse;
import io.smallrye.graphql.execution.error.ExecutionErrorsService;
import io.smallrye.mutiny.Multi;
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
    private final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

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
                serverWebSocket.closeHandler(new CloseHandler());
                serverWebSocket.endHandler(new EndHandler());
                serverWebSocket.exceptionHandler(new ExceptionHandler());
                serverWebSocket.textMessageHandler(new TextMessageHandler(serverWebSocket));
            }
        }
    }

    private class CloseHandler implements Handler<Void> {
        @Override
        public void handle(Void e) {
            unsubscribe();
        }
    }

    private class EndHandler implements Handler<Void> {
        @Override
        public void handle(Void e) {
            unsubscribe();
        }
    }

    private class ExceptionHandler implements Handler<Throwable> {
        @Override
        public void handle(Throwable e) {
            log.error(e.getMessage());
            unsubscribe();
        }
    }

    public void unsubscribe() {
        if (subscriptionRef.get() != null) {
            Subscriptions.cancel(subscriptionRef);
            subscriptionRef.set(null);
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

                        Multi<ExecutionResult> multiStream = Multi.createFrom().publisher(stream);

                        multiStream.onFailure().recoverWithItem(failure -> {
                            // TODO: Below must move to SmallRye, and once 1.2.1 is releaes we can remove it here
                            //       Once in SmallRye, this will also follow the propper error rule (show/hide) and add more details.
                            return new ExecutionResultImpl(GraphqlErrorBuilder.newError()
                                    .message(failure.getMessage())
                                    .build());
                        }).subscribe(smallRyeGraphQLSubscriptionSubscriber);
                    }
                }
            }
        }
    }

    private class SmallRyeGraphQLSubscriptionSubscriber implements Subscriber<ExecutionResult> {
        private final ServerWebSocket serverWebSocket;

        public SmallRyeGraphQLSubscriptionSubscriber(ServerWebSocket serverWebSocket) {
            this.serverWebSocket = serverWebSocket;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (subscriptionRef.compareAndSet(null, s)) {
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
                Subscription s = subscriptionRef.get();
                s.request(1);
            } else {
                // Connection to client is closed, but we still receive mesages
                unsubscribe();
            }
        }

        @Override
        public void onError(Throwable thrwbl) {
            log.error("Error in GraphQL Subscription Websocket", thrwbl);
            unsubscribe();
            closeWebSocket();
        }

        @Override
        public void onComplete() {
            unsubscribe();
            closeWebSocket();
        }

        public void closeWebSocket() {
            if (!serverWebSocket.isClosed()) {
                serverWebSocket.close();
            }
        }
    }
}
