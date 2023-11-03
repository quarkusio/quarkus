package io.quarkus.grpc.stubs;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class ManyToManyObserver<I, O> extends AbstractMulti<O> implements StreamObserver<O> {

    private final StreamObserver<I> processor;
    private final Multi<I> source;
    private final UpstreamSubscriber subscriber = new UpstreamSubscriber();
    private final AtomicReference<Flow.Subscription> upstream = new AtomicReference<>();
    private volatile MultiSubscriber<? super O> downstream;

    public ManyToManyObserver(Multi<I> source, Function<StreamObserver<O>, StreamObserver<I>> function) {
        this.processor = function.apply(this);
        this.source = source;
    }

    @Override
    public void subscribe(MultiSubscriber<? super O> subscriber) {
        this.downstream = subscriber;
        source.subscribe(this.subscriber);
    }

    @Override
    public void onNext(O value) {
        downstream.onItem(value);
    }

    @Override
    public void onError(Throwable t) {
        cancelUpstream();
        downstream.onFailure(t);
    }

    @Override
    public void onCompleted() {
        cancelUpstream();
        downstream.onComplete();
    }

    private void cancelUpstream() {
        var subscription = upstream.getAndSet(Subscriptions.CANCELLED);
        if (subscription != null) {
            subscription.cancel();
        }
    }

    class UpstreamSubscriber implements Flow.Subscriber<I>, Flow.Subscription {

        @Override
        public void onSubscribe(Flow.Subscription items) {
            if (!upstream.compareAndSet(null, items)) {
                items.cancel();
            } else {
                downstream.onSubscribe(this);
                items.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(I item) {
            processor.onNext(item);
        }

        @Override
        public void onError(Throwable t) {
            processor.onError(t);
        }

        @Override
        public void onComplete() {
            processor.onCompleted();
        }

        @Override
        public void request(long n) {
            // Ignored
            // TODO We would need to implement back-pressure, but there is no relation between the number of items
            // TODO from the source and the number of items from the service.
        }

        @Override
        public void cancel() {
            cancelUpstream();
        }
    }

}
