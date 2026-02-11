package io.quarkus.grpc.stubs;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public abstract class MutinyClientStreamObserver<ReqT, RespT> implements ClientResponseObserver<ReqT, RespT> {

    MultiSubscriber<ReqT> subscriber;

    @Override
    public void beforeStart(ClientCallStreamObserver<ReqT> requestStream) {
        // create this before requestStream is frozen
        subscriber = new MultiToClientCallStreamObserverSubscriber<ReqT>(requestStream, getPrefetch());
    }

    // TODO -- cache this?
    private static long getPrefetch() {
        Config config = ConfigProvider.getConfig();
        Optional<Long> opt = config.getOptionalValue("quarkus.grpc.client.prefetch", Long.class);
        return opt.orElse(1000L);
    }

}
