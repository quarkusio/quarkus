package io.quarkus.resteasy.mutiny.common.runtime;

import org.jboss.resteasy.spi.AsyncStreamProvider;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

public class MultiProvider implements AsyncStreamProvider<Multi<?>> {
    @Override
    public Publisher<?> toAsyncStream(Multi<?> multi) {
        return AdaptersToReactiveStreams.publisher(multi);
    }
}
