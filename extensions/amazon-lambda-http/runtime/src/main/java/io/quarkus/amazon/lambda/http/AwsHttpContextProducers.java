package io.quarkus.amazon.lambda.http;

import java.util.Collections;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.QuarkusHttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class AwsHttpContextProducers {

    @RequestScoped
    @Produces
    public Context getAwsContext() {
        return (Context) getContextObjects().get(Context.class);
    }

    @RequestScoped
    @Produces
    public APIGatewayV2HTTPEvent getHttpEvent() {
        return (APIGatewayV2HTTPEvent) getContextObjects().get(APIGatewayV2HTTPEvent.class);
    }

    @RequestScoped
    @Produces
    public APIGatewayV2HTTPEvent.RequestContext getHttpRequestContext() {
        return (APIGatewayV2HTTPEvent.RequestContext) getContextObjects().get(APIGatewayV2HTTPEvent.RequestContext.class);
    }

    @Inject
    Instance<CurrentVertxRequest> current;

    private Map<Class<?>, Object> getContextObjects() {
        if (current == null) {
            return Collections.EMPTY_MAP;
        }
        CurrentVertxRequest currentVertxRequest = current.get();
        if (currentVertxRequest == null) {
            return Collections.EMPTY_MAP;
        }
        RoutingContext routingContext = currentVertxRequest.getCurrent();
        if (routingContext == null) {
            return Collections.EMPTY_MAP;
        }
        MultiMap qheaders = routingContext.request().headers();
        if (qheaders == null) {
            return Collections.EMPTY_MAP;
        }
        if (qheaders instanceof QuarkusHttpHeaders) {
            return ((QuarkusHttpHeaders) qheaders).getContextObjects();
        } else {
            return Collections.EMPTY_MAP;
        }
    }

}
