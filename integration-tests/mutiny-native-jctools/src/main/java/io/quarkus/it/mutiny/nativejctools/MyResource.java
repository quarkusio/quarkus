package io.quarkus.it.mutiny.nativejctools;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestStreamElementType;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiEmitter;

@Path("/tests")
public class MyResource {

    @GET
    @Path("create-queues")
    public String createQueues() {
        ArrayList<Long> sums = new ArrayList<>();
        List.of(
                Queues.createMpscQueue(),
                Queues.createMpscArrayQueue(2048),
                Queues.createSpscArrayQueue(2048),
                Queues.createSpscUnboundedQueue(2048),
                Queues.createSpscUnboundedArrayQueue(2048),
                Queues.createSpscChunkedArrayQueue(2048)).forEach(queue -> {
                    for (int i = 0; i < 1024; i++) {
                        queue.offer(i);
                    }
                    long sum = 0L;
                    for (Integer n = (Integer) queue.poll(); n != null; n = (Integer) queue.poll()) {
                        sum = sum + n;
                    }
                    sums.add(sum);
                });
        return "Ok :: " + sums.stream()
                .map(Objects::toString)
                .collect(Collectors.joining("/"));
    }

    @GET
    @Path("ticks-overflow")
    public String ticksWithOverflow() {
        List<String> ticks = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .onOverflow().bufferUnconditionally()
                .onItem().transform(tick -> ":")
                .select().first(10)
                .collect().asList().await().atMost(Duration.ofSeconds(5));
        return String.join("", ticks);
    }

    @GET
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @Path("emitter")
    public Multi<String> emitter() {
        AtomicInteger counter = new AtomicInteger();
        return Multi.createFrom().emitter(emitter -> {
            ScheduledExecutorService scheduler = Infrastructure.getDefaultWorkerPool();
            scheduler.schedule(() -> emitAndSchedule(counter, emitter), 100, TimeUnit.MILLISECONDS);
        });
    }

    private void emitAndSchedule(AtomicInteger counter, MultiEmitter<? super String> emitter) {
        ScheduledExecutorService scheduler = Infrastructure.getDefaultWorkerPool();
        int n = counter.getAndIncrement();
        if (n < 5) {
            for (int i = 0; i < n * 10; i++) {
                emitter.emit(String.valueOf(i));
            }
            scheduler.schedule(() -> emitAndSchedule(counter, emitter), 125, TimeUnit.MILLISECONDS);
        } else {
            emitter.complete();
        }
    }
}
