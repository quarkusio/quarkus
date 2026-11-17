package org.jboss.resteasy.reactive.client.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.spies.MultiOnCancellationSpy;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiEmitter;

/**
 * The cancel action of a {@link MultiInvoker.MultiRequest} runs once, also when the subscriber cancels before the
 * action is registered.
 */
public class MultiRequestTest {

    @Test
    public void cancelBeforeRegistrationRunsTheAction() {
        AtomicReference<MultiInvoker.MultiRequest<String>> request = new AtomicReference<>();
        MultiOnCancellationSpy<String> spy = Spy.onCancellation(Multi.createFrom()
                .<String> emitter(e -> request.set(new MultiInvoker.MultiRequest<>(e))));
        AssertSubscriber<String> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(1));
        subscriber.cancel();
        assertTrue(spy.invoked());
        assertTrue(request.get().isCancelled());
        AtomicInteger calls = new AtomicInteger();
        request.get().onCancel(calls::incrementAndGet);
        assertEquals(1, calls.get());
    }

    @Test
    public void cancelAfterRegistrationRunsTheActionOnce() {
        AtomicReference<MultiInvoker.MultiRequest<String>> request = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        MultiOnCancellationSpy<String> spy = Spy.onCancellation(Multi.createFrom().<String> emitter(e -> {
            request.set(new MultiInvoker.MultiRequest<>(e));
            request.get().onCancel(calls::incrementAndGet);
        }));
        AssertSubscriber<String> subscriber = spy.subscribe().withSubscriber(AssertSubscriber.create(1));
        subscriber.cancel();
        subscriber.cancel();
        assertEquals(1, spy.invocationCount());
        assertEquals(1, calls.get());
    }

    @Test
    public void completeRunsTheActionAndDropsLaterItems() {
        AtomicReference<MultiInvoker.MultiRequest<String>> request = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        AssertSubscriber<String> subscriber = Multi.createFrom().<String> emitter(e -> {
            request.set(new MultiInvoker.MultiRequest<>(e));
            request.get().onCancel(calls::incrementAndGet);
        }).subscribe().withSubscriber(AssertSubscriber.create(10));
        request.get().emit("a");
        request.get().complete();
        request.get().emit("b");
        subscriber.assertItems("a").assertCompleted();
        assertEquals(1, calls.get());
    }

    @Test
    public void completingTheEmitterDirectlyRunsTheActionOnce() {
        AtomicReference<MultiEmitter<? super String>> emitter = new AtomicReference<>();
        List<String> events = new CopyOnWriteArrayList<>();
        AssertSubscriber<String> subscriber = Multi.createFrom().<String> emitter(e -> {
            emitter.set(e);
            new MultiInvoker.MultiRequest<>(e).onCancel(() -> events.add("cancel action"));
        }).subscribe().withSubscriber(AssertSubscriber.create(10));
        emitter.get().complete();
        subscriber.assertCompleted();
        assertEquals(List.of("cancel action"), events);
    }
}
