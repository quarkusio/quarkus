package io.quarkus.hibernate.validator.runtime.locale;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

/**
 * Locale resolver that retrieves the locale from HTTP headers of the current vert.x request.
 * Currently used for handling GraphQL requests.
 */
@Singleton
public class ResteasyReactiveLocaleResolver extends AbstractLocaleResolver {

    @Inject
    CurrentVertxRequest currentVertxRequest;

    @Override
    protected Map<String, List<String>> getHeaders() {
        RoutingContext current = currentVertxRequest.getCurrent();
        if (current != null) {
            Map<String, List<String>> result = new HashMap<>();
            MultiMap headers = current.request().headers();
            for (String name : headers.names()) {
                result.put(name, headers.getAll(name));
            }
            return result;
        } else {
            return null;
        }
    }

}
