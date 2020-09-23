package io.quarkus.rest.runtime.core.parameters;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.injection.QuarkusRestInjectionTarget;
import io.quarkus.rest.runtime.spi.BeanFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class BeanParamExtractor implements ParameterExtractor {

    private final BeanFactory<Object> factory;

    public BeanParamExtractor(BeanFactory<Object> factory) {
        this.factory = factory;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
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
