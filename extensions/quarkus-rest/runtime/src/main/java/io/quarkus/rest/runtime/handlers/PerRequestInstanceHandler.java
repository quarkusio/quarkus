package io.quarkus.rest.runtime.handlers;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.spi.BeanFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class PerRequestInstanceHandler implements RestHandler {

    /**
     * CDI Manages the lifecycle. If this is a per request resource then this will be a client proxy
     *
     */
    private final BeanFactory<Object> factory;

    public PerRequestInstanceHandler(BeanFactory<Object> factory) {
        this.factory = factory;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        BeanFactory.BeanInstance<Object> instance = factory.createInstance();
        requestContext.setEndpointInstance(instance.getInstance());
        requestContext.getContext().addEndHandler(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                instance.close();
            }
        });
    }
}
