package io.quarkus.rest.server.runtime;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.handlers.ServerRestHandler;
import org.jboss.resteasy.reactive.server.injection.QuarkusRestInjectionTarget;
import org.jboss.resteasy.reactive.spi.BeanFactory;

import io.quarkus.arc.runtime.ClientProxyUnwrapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class PerRequestInstanceHandler implements ServerRestHandler {

    private static final ClientProxyUnwrapper CLIENT_PROXY_UNWRAPPER = new ClientProxyUnwrapper();

    /**
     * CDI Manages the lifecycle. If this is a per request resource then this will be a client proxy
     *
     */
    private final BeanFactory<Object> factory;

    public PerRequestInstanceHandler(BeanFactory<Object> factory) {
        this.factory = factory;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        BeanFactory.BeanInstance<Object> instance = factory.createInstance();
        requestContext.setEndpointInstance(instance.getInstance());
        ((QuarkusRestInjectionTarget) CLIENT_PROXY_UNWRAPPER.apply(instance.getInstance()))
                .__quarkus_rest_inject(requestContext);
        requestContext.getContext().addEndHandler(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                instance.close();
            }
        });
    }
}
