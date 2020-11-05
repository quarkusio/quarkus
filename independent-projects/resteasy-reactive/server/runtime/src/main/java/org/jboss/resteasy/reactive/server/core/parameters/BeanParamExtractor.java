package org.jboss.resteasy.reactive.server.core.parameters;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.injection.QuarkusRestInjectionTarget;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class BeanParamExtractor implements ParameterExtractor {

    private final BeanFactory<Object> factory;

    public BeanParamExtractor(BeanFactory<Object> factory) {
        this.factory = factory;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        BeanFactory.BeanInstance<Object> instance = factory.createInstance();
        context.getContext().addEndHandler(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                instance.close();
            }
        });
        ((QuarkusRestInjectionTarget) instance.getInstance()).__quarkus_rest_inject(context);
        return instance.getInstance();
    }
}
