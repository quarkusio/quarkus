package org.jboss.resteasy.reactive.client.impl;

import io.smallrye.mutiny.Uni;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;

public class UniInvoker extends AbstractRxInvoker<Uni<?>> {

    private WebTargetImpl target;

    public UniInvoker(WebTargetImpl target) {
        this.target = target;
    }

    @Override
    public <R> Uni<R> method(String name, Entity<?> entity, GenericType<R> responseType) {
        AsyncInvokerImpl invoker = (AsyncInvokerImpl) target.request().rx();
        return Uni.createFrom().completionStage(invoker.method(name, entity, responseType));
    }

}
