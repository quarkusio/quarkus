package io.quarkus.amazon.lambda.http;

import java.util.Collections;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.amazonaws.services.lambda.runtime.Context;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequestContext;
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
    public AwsProxyRequestContext getHttpEvent() {
        return (AwsProxyRequestContext) getContextObjects().get(AwsProxyRequestContext.class);
    }

    @RequestScoped
    @Produces
    public AwsProxyRequest getHttpRequestContext() {
        return (AwsProxyRequest) getContextObjects().get(AwsProxyRequest.class);
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
