package io.quarkus.vertx.http.runtime.attribute;

import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

/**
 * The remote user
 *
 */
public class RemoteUserAttribute implements ExchangeAttribute {

    public static final String REMOTE_USER_SHORT = "%u";
    public static final String REMOTE_USER = "%{REMOTE_USER}";
    public static final String VERTX_USER_NAME = "username";

    public static final ExchangeAttribute INSTANCE = new RemoteUserAttribute();

    private RemoteUserAttribute() {

    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        User user = exchange.user();
        if (user == null) {
            return null;
        }
        if (user instanceof QuarkusHttpUser quarkusUser) {
            return quarkusUser.getSecurityIdentity().getPrincipal().getName();
        } else {
            return user.principal().getString(VERTX_USER_NAME);
        }
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Remote user", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Remote user";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REMOTE_USER) || token.equals(REMOTE_USER_SHORT)) {
                return RemoteUserAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
