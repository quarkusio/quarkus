package org.jboss.resteasy.reactive.server.handlers;

import java.util.function.Function;
import javax.ws.rs.container.CompletionCallback;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.injection.QuarkusRestInjectionTarget;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class PerRequestInstanceHandler implements ServerRestHandler {

    /**
     * CDI Manages the lifecycle. If this is a per request resource then this will be a client proxy
     *
     */
    private final BeanFactory<Object> factory;
    private final Function<Object, Object> clientProxyUnwrapper;

    public PerRequestInstanceHandler(BeanFactory<Object> factory, Function<Object, Object> clientProxyUnwrapper) {
        this.factory = factory;
        this.clientProxyUnwrapper = clientProxyUnwrapper;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        BeanFactory.BeanInstance<Object> instance = factory.createInstance();
        requestContext.setEndpointInstance(instance.getInstance());
        ((QuarkusRestInjectionTarget) clientProxyUnwrapper.apply(instance.getInstance()))
                .__quarkus_rest_inject(requestContext);
        requestContext.registerCompletionCallback(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                instance.close();
            }
        });
    }
}
