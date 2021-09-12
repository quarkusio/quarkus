package io.quarkus.vertx.web.runtime;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Just a wrapped to capture the fact that the items must be written as SSE.
 * 
 * @param <T> the type of item.
 */
public class SSEMulti<T> extends AbstractMulti<T> {

    private final Multi<T> multi;

    public SSEMulti(Multi<T> multi) {
        this.multi = multi;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        multi.subscribe(Infrastructure.onMultiSubscription(multi, subscriber));
    }

}
