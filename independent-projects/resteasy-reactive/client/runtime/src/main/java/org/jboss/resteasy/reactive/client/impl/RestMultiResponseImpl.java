package org.jboss.resteasy.reactive.client.impl;

import org.jboss.resteasy.reactive.client.BasicRestResponse;
import org.jboss.resteasy.reactive.client.RestMultiResponse;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

class RestMultiResponseImpl<T> extends AbstractMulti<T> implements RestMultiResponse<T> {

    private final Multi<T> delegate;
    private final Uni<BasicRestResponse> responseUni;

    RestMultiResponseImpl(Multi<T> delegate, Uni<BasicRestResponse> responseUni) {
        this.delegate = delegate;
        this.responseUni = responseUni;
    }

    @Override
    public Uni<BasicRestResponse> response() {
        return responseUni;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        delegate.subscribe(subscriber);
    }
}
