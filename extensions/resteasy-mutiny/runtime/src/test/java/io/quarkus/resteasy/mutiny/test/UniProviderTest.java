package io.quarkus.resteasy.mutiny.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import io.quarkus.resteasy.mutiny.runtime.UniProvider;
import io.smallrye.mutiny.Uni;

public class UniProviderTest {

    private final UniProvider provider = new UniProvider();

    @Test
    public void testFromCompletionStage() {
        final CompletableFuture<Integer> cs = new CompletableFuture<>();
        cs.complete(1);
        final Uni<?> uni = provider.fromCompletionStage(cs);
        assertEquals(1, uni.await().indefinitely());
    }

    @Test
    public void testToCompletionStageCase() {
        final Object actual = provider.toCompletionStage(Uni.createFrom().item(1)).toCompletableFuture().join();
        assertEquals(1, actual);
    }

    @Test
    public void testToCompletionStageNullCase() {
        final CompletableFuture<Integer> cs = new CompletableFuture<>();
        cs.complete(null);
        final Uni<?> uni = Uni.createFrom().completionStage(cs);
        final Object actual = provider.toCompletionStage(uni).toCompletableFuture().join();
        assertNull(actual);
    }

}
