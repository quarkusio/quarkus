package io.quarkus.resteasy.mutiny.common.test;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.quarkus.resteasy.mutiny.common.runtime.MultiProvider;
import io.smallrye.mutiny.Multi;

public class MultiProviderTest {

    private final MultiProvider provider = new MultiProvider();

    @Test
    public void test() {
        Multi<?> multi = Multi.createFrom().items(1, 2, 3);
        Publisher<?> publisher = provider.toAsyncStream(multi);
        List<?> list = Multi.createFrom().publisher(publisher).collect().asList().await().indefinitely();
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(2, list.get(1));
        Assertions.assertEquals(3, list.get(2));
    }

}
