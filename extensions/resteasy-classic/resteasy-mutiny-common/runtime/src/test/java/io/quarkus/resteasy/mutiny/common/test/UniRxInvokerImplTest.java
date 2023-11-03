package io.quarkus.resteasy.mutiny.common.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.client.CompletionStageRxInvoker;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.resteasy.mutiny.common.runtime.UniRxInvokerImpl;
import io.smallrye.mutiny.Uni;

@SuppressWarnings("unchecked")
public class UniRxInvokerImplTest {

    @Test
    public void testGetEntity() {
        AtomicInteger counter = new AtomicInteger();
        CompletionStageRxInvoker csInvoker = mock(CompletionStageRxInvoker.class);
        UniRxInvokerImpl invoker = new UniRxInvokerImpl(csInvoker);
        when(csInvoker.get(Integer.class)).thenReturn(
                CompletableFuture.completedFuture(counter.incrementAndGet()), // First call
                CompletableFuture.completedFuture(counter.incrementAndGet()) // Second call
        );

        Uni<Integer> uni = invoker.get(Integer.class);
        Assertions.assertEquals(1, uni.await().indefinitely());
        Assertions.assertEquals(2, uni.await().indefinitely());
    }

    @Test
    public void testGet() {
        AtomicInteger counter = new AtomicInteger();
        CompletionStageRxInvoker csInvoker = mock(CompletionStageRxInvoker.class);
        UniRxInvokerImpl invoker = new UniRxInvokerImpl(csInvoker);

        CompletionStage<Response> resp1 = CompletableFuture
                .completedFuture(Response.ok(counter.incrementAndGet()).build());
        CompletionStage<Response> resp2 = CompletableFuture
                .completedFuture(Response.ok(counter.incrementAndGet()).build());
        when(csInvoker.get()).thenReturn(
                resp1, resp2);

        Uni<Integer> uni = invoker.get()
                .onItem().transform(r -> r.readEntity(Integer.class));
        Assertions.assertEquals(1, uni.await().indefinitely());
        Assertions.assertEquals(2, uni.await().indefinitely());
    }

    @Test
    public void testGetWithRetry() {
        AtomicInteger counter = new AtomicInteger();
        CompletionStageRxInvoker csInvoker = mock(CompletionStageRxInvoker.class);
        UniRxInvokerImpl invoker = new UniRxInvokerImpl(csInvoker);

        CompletableFuture<Response> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IOException("boom"));
        CompletionStage<Response> ok = CompletableFuture
                .completedFuture(Response.ok(counter.incrementAndGet()).build());
        when(csInvoker.get()).thenReturn(failed, ok);

        Uni<Integer> uni = invoker.get()
                .onItem().transform(r -> r.readEntity(Integer.class))
                .onFailure().retry().atMost(1);
        Assertions.assertEquals(1, uni.await().indefinitely());
    }

    @Test
    public void testPut() {
        AtomicInteger counter = new AtomicInteger();
        CompletionStageRxInvoker csInvoker = mock(CompletionStageRxInvoker.class);
        UniRxInvokerImpl invoker = new UniRxInvokerImpl(csInvoker);

        CompletionStage<Response> resp1 = CompletableFuture
                .completedFuture(Response.ok(counter.incrementAndGet()).build());
        CompletionStage<Response> resp2 = CompletableFuture
                .completedFuture(Response.ok(counter.incrementAndGet()).build());
        Entity<String> entity = Entity.entity("hello", MediaType.TEXT_PLAIN_TYPE);
        when(csInvoker.put(entity)).thenReturn(
                resp1, resp2);

        Uni<Integer> uni = invoker.put(entity)
                .onItem().transform(r -> r.readEntity(Integer.class));
        Assertions.assertEquals(1, uni.await().indefinitely());
        Assertions.assertEquals(2, uni.await().indefinitely());
    }

    @Test
    public void testPostEntity() {
        AtomicInteger counter = new AtomicInteger();
        CompletionStageRxInvoker csInvoker = mock(CompletionStageRxInvoker.class);
        UniRxInvokerImpl invoker = new UniRxInvokerImpl(csInvoker);
        Entity<String> entity = Entity.entity("hello", MediaType.TEXT_PLAIN_TYPE);
        when(csInvoker.post(entity, Integer.class)).thenReturn(
                CompletableFuture.completedFuture(counter.incrementAndGet()), // First call
                CompletableFuture.completedFuture(counter.incrementAndGet()) // Second call
        );

        Uni<Integer> uni = invoker.post(entity, Integer.class);
        Assertions.assertEquals(1, uni.await().indefinitely());
        Assertions.assertEquals(2, uni.await().indefinitely());
    }

    @Test
    public void testDeleteEntity() {
        AtomicInteger counter = new AtomicInteger();
        CompletionStageRxInvoker csInvoker = mock(CompletionStageRxInvoker.class);
        UniRxInvokerImpl invoker = new UniRxInvokerImpl(csInvoker);
        when(csInvoker.delete(Integer.class)).thenReturn(
                CompletableFuture.completedFuture(counter.incrementAndGet()), // First call
                CompletableFuture.completedFuture(counter.incrementAndGet()) // Second call
        );

        Uni<Integer> uni = invoker.delete(Integer.class);
        Assertions.assertEquals(1, uni.await().indefinitely());
        Assertions.assertEquals(2, uni.await().indefinitely());
    }

    @Test
    public void testHead() {
        AtomicInteger counter = new AtomicInteger();
        CompletionStageRxInvoker csInvoker = mock(CompletionStageRxInvoker.class);
        UniRxInvokerImpl invoker = new UniRxInvokerImpl(csInvoker);

        CompletionStage<Response> resp1 = CompletableFuture
                .completedFuture(Response.ok(counter.incrementAndGet()).build());
        CompletionStage<Response> resp2 = CompletableFuture
                .completedFuture(Response.ok(counter.incrementAndGet()).build());
        when(csInvoker.head()).thenReturn(
                resp1, resp2);

        Uni<Integer> uni = invoker.head()
                .onItem().transform(r -> r.readEntity(Integer.class));
        Assertions.assertEquals(1, uni.await().indefinitely());
        Assertions.assertEquals(2, uni.await().indefinitely());
    }

}
