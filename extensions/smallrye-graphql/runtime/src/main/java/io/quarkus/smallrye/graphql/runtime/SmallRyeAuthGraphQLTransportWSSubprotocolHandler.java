package io.quarkus.smallrye.graphql.runtime;

import java.util.Map;
import java.util.Optional;

import jakarta.json.JsonObject;

import io.quarkus.smallrye.graphql.runtime.exception.SmallRyeAuthSecurityIdentityAlreadyAssignedException;
import io.smallrye.graphql.websocket.GraphQLWebSocketSession;
import io.smallrye.graphql.websocket.graphqltransportws.GraphQLTransportWSSubprotocolHandler;
import io.vertx.ext.web.RoutingContext;

/**
 * An extension over GraphQLTransportWSSubprotocolHandler which supports defining an Authorization header in the client init
 * message.
 * This key of the Authorization header is defined by the `authorizationClientInitPayloadName` property.
 */
public class SmallRyeAuthGraphQLTransportWSSubprotocolHandler extends GraphQLTransportWSSubprotocolHandler {

    private final SmallRyeAuthGraphQLWSHandler authHander;

    public SmallRyeAuthGraphQLTransportWSSubprotocolHandler(GraphQLWebSocketSession session,
            Map<String, Object> context,
            RoutingContext ctx,
            SmallRyeGraphQLAbstractHandler handler,
            Optional<String> authorizationClientInitPayloadName) {
        super(session, context);

        this.authHander = new SmallRyeAuthGraphQLWSHandler(session, ctx, handler, authorizationClientInitPayloadName);
    }

    @Override
    protected void onMessage(JsonObject message) {
        if (message != null && message.getString("type").equals("connection_init")) {
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");

            this.authHander.handlePayload(payload, () -> {
                // Identity has been updated. Now pass the successful connection_init back to the SmallRye GraphQL library to take over
                super.onMessage(message);
            }, failure -> {
                // Failure handling Authorization. This method triggers a 4401 (Unauthorized).
                if (!session.isClosed()) {
                    if (failure instanceof SmallRyeAuthSecurityIdentityAlreadyAssignedException) {
                        session.close((short) 4400, "Authorization specified in multiple locations");
                    } else {
                        session.close((short) 4403, "Forbidden");
                    }
                }
            });
        } else {
            super.onMessage(message);
        }
    }

    @Override
    public void onClose() {
        super.onClose();

        this.authHander.cancelAuthExpiry();
    }
}
