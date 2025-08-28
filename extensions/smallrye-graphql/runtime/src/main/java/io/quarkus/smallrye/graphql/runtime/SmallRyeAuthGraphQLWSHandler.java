package io.quarkus.smallrye.graphql.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.json.JsonString;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.smallrye.graphql.runtime.exception.SmallRyeAuthSecurityIdentityAlreadyAssignedException;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.graphql.websocket.GraphQLWebSocketSession;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.ext.web.RoutingContext;

/**
 * A class which is capable of performing authentication against connection_init payloads for all GraphQL WebSocket
 * subprotocols.
 * Is also responsible for handling the closing of WebSockets when authentication expires.
 */
public class SmallRyeAuthGraphQLWSHandler {

    private final GraphQLWebSocketSession session;
    private final RoutingContext ctx;
    private final SmallRyeGraphQLAbstractHandler handler;
    private final Optional<String> authorizationClientInitPayloadName;
    private final AtomicReference<Cancellable> authExpiry = new AtomicReference<>();

    public SmallRyeAuthGraphQLWSHandler(GraphQLWebSocketSession session,
            RoutingContext ctx,
            SmallRyeGraphQLAbstractHandler handler,
            Optional<String> authorizationClientInitPayloadName) {
        this.session = session;
        this.ctx = ctx;
        this.handler = handler;
        this.authorizationClientInitPayloadName = authorizationClientInitPayloadName;

        updateIdentityExpiry(ctx);
    }

    /**
     * Takes the payload sent from connection_init, and if quarkus.smallrye-graphql.authorization-client-init-payload-name is
     * defined,
     * then will take the value from the payload that matches the configured value. The value is treated as an Authorization
     * header.
     *
     * @param payload The connection_init payload
     * @param successCallback Will back called once the authentication has completed (or is skipped if not required)
     * @param failureHandler Any errors which occur during authentication will be passed to this handler
     */
    void handlePayload(Map<String, Object> payload, Runnable successCallback, Consumer<Throwable> failureHandler) {
        if (authorizationClientInitPayloadName.isPresent() && payload != null &&
                payload.get(authorizationClientInitPayloadName.get()) instanceof JsonString authorizationJson) {
            String authorization = authorizationJson.getString();

            HttpAuthenticator authenticator = ctx.get(HttpAuthenticator.class.getName());
            // Get the existing identity of the RoutingContext
            QuarkusHttpUser.getSecurityIdentity(ctx, authenticator.getIdentityProviderManager()).subscribe()
                    .withSubscriber(new UniSubscriber<SecurityIdentity>() {
                        @Override
                        public void onSubscribe(UniSubscription subscription) {

                        }

                        @Override
                        public void onItem(SecurityIdentity previousIdentity) {
                            if (!previousIdentity.isAnonymous()) {
                                // Fail if this WebSocket already has an identity assigned to it
                                onFailure(new SmallRyeAuthSecurityIdentityAlreadyAssignedException(payload));

                                return;
                            } else if (ctx.user() == null) {
                                // Ensure that the identity which we have now got is actually set against the RoutingContext.
                                QuarkusHttpUser.setIdentity(previousIdentity, ctx);
                            }

                            // Treat the Authorization sent in the client init in the same was that it would be if sent in the request header
                            ctx.request().headers().add("Authorization", authorization);

                            Uni<SecurityIdentity> potentialUser = authenticator.attemptAuthentication(ctx);
                            potentialUser
                                    .subscribe().withSubscriber(new UniSubscriber<SecurityIdentity>() {
                                        @Override
                                        public void onSubscribe(UniSubscription subscription) {

                                        }

                                        @Override
                                        public void onItem(SecurityIdentity identity) {
                                            // If identity is null, we've set the anonymous identity above, so we can ignore it
                                            if (!session.isClosed() && identity != null) {
                                                QuarkusHttpUser.setIdentity(identity, ctx);
                                                handler.withIdentity(ctx);

                                                updateIdentityExpiry(ctx);

                                                successCallback.run();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Throwable failure) {
                                            BiConsumer<RoutingContext, Throwable> handler = ctx
                                                    .get(QuarkusHttpUser.AUTH_FAILURE_HANDLER);
                                            if (handler != null) {
                                                handler.accept(ctx, failure);
                                            }
                                            failureHandler.accept(failure);
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(Throwable failure) {
                            BiConsumer<RoutingContext, Throwable> handler = ctx
                                    .get(QuarkusHttpUser.AUTH_FAILURE_HANDLER);
                            if (handler != null) {
                                handler.accept(ctx, failure);
                            }
                            failureHandler.accept(failure);
                        }
                    });
        } else {
            successCallback.run();
        }
    }

    private void updateIdentityExpiry(RoutingContext ctx) {
        Cancellable replacing = authExpiry.getAndSet(createAuthExpiryTask(ctx, session));
        if (replacing != null) {
            // Should never replace an expiring identity.
            // As above, we only permit updating the identity if the session starts with an anonymous identity.
            replacing.cancel();
        }
    }

    public void cancelAuthExpiry() {
        Cancellable expire = authExpiry.get();
        if (expire != null) {
            expire.cancel();
        }
    }

    public static Cancellable createAuthExpiryTask(RoutingContext ctx, GraphQLWebSocketSession webSocketSession) {
        AtomicLong atomicTaskId = new AtomicLong(-1);

        Cancellable sub = QuarkusHttpUser.getSecurityIdentity(ctx, null)
                .subscribe()
                .with(securityIdentity -> {
                    QuarkusHttpUser user = (QuarkusHttpUser) ctx.user();
                    if (user != null) {
                        //close the connection when the identity expires
                        Long expire = user.getSecurityIdentity().getAttribute("quarkus.identity.expire-time");
                        if (expire != null) {
                            long taskId = ctx.vertx().setTimer((expire * 1000) - System.currentTimeMillis(),
                                    event -> {
                                        if (!webSocketSession.isClosed()) {
                                            webSocketSession.close((short) 1008, "Authentication expired");
                                        }
                                    });
                            long previousValue = atomicTaskId.getAndSet(taskId);
                            if (previousValue == -2) {
                                // If -2 it's been cancelled.
                                ctx.vertx().cancelTimer(taskId);
                            }
                        }
                    }
                });

        return () -> {
            // If we're still loading the deferred identity, cancel
            sub.cancel();

            // Set to -2 to indicate it's been cancelled.
            long taskId = atomicTaskId.getAndSet(-2);

            if (taskId >= 0) {
                ctx.vertx().cancelTimer(taskId);
            }
        };
    }
}
