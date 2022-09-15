package org.jboss.resteasy.reactive.server.core.parameters;

import jakarta.ws.rs.container.CompletionCallback;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.injection.ResteasyReactiveInjectionTarget;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class InjectParamExtractor implements ParameterExtractor {

    private final BeanFactory<Object> factory;

    public InjectParamExtractor(BeanFactory<Object> factory) {
        this.factory = factory;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        BeanFactory.BeanInstance<Object> instance = factory.createInstance();
        context.registerCompletionCallback(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                instance.close();
            }
        });
        ((ResteasyReactiveInjectionTarget) instance.getInstance()).__quarkus_rest_inject(context);
        return instance.getInstance();
    }
}
