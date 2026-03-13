package io.quarkus.grpc.stubs;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

import io.grpc.stub.ClientCallStreamObserver;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiToClientCallStreamObserverSubscriber<O> implements MultiSubscriber<O> {

    private final ClientCallStreamObserver<O> streamObserver;
    private final long prefetch;

    private Flow.Subscription upstream;
    private final AtomicLong demand = new AtomicLong();

    public MultiToClientCallStreamObserverSubscriber(ClientCallStreamObserver<O> streamObserver, long prefetch) {
        this.streamObserver = streamObserver;
        this.streamObserver.setOnReadyHandler(this::observerIsReady);
        this.prefetch = prefetch;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.upstream = subscription;
    }

    private void observerIsReady() {
        if (streamObserver.isReady() && demand.get() == 0L) {
            fetchMoreFromUpstream();
        }
    }

    private void fetchMoreFromUpstream() {
        // ready handler can be called before onSubscribe
        if (upstream != null) {
            demand.addAndGet(prefetch);
            upstream.request(prefetch);
        }
    }

    @Override
    public void onItem(O item) {
        if (demand.decrementAndGet() <= 0L && streamObserver.isReady()) {
            fetchMoreFromUpstream();
        }
        streamObserver.onNext(item);
    }

    @Override
    public void onFailure(Throwable failure) {
        streamObserver.onError(failure);
    }

    @Override
    public void onCompletion() {
        streamObserver.onCompleted();
    }
}
