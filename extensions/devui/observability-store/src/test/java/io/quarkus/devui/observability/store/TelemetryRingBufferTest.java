package io.quarkus.devui.observability.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class TelemetryRingBufferTest {

    @Test
    void evictsOldestBeyondCapacity() {
        TelemetryRingBuffer<Integer> buffer = new TelemetryRingBuffer<>(new CountBoundingStrategy(3));
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.add(4); // evicts 1
        assertThat(buffer.snapshot()).containsExactly(2, 3, 4);
        assertThat(buffer.size()).isEqualTo(3);
    }

    @Test
    void snapshotIsAnIndependentCopy() {
        TelemetryRingBuffer<Integer> buffer = new TelemetryRingBuffer<>(new CountBoundingStrategy(10));
        buffer.add(1);
        List<Integer> snap = buffer.snapshot();
        buffer.add(2);
        assertThat(snap).containsExactly(1); // snapshot not affected by later adds
    }

    @Test
    void clearEmptiesBuffer() {
        TelemetryRingBuffer<Integer> buffer = new TelemetryRingBuffer<>(new CountBoundingStrategy(10));
        buffer.add(1);
        buffer.clear();
        assertThat(buffer.size()).isZero();
        assertThat(buffer.snapshot()).isEmpty();
    }

    @Test
    void concurrentAddsKeepBufferCappedAndConsistent() throws InterruptedException {
        TelemetryRingBuffer<Integer> buffer = new TelemetryRingBuffer<>(new CountBoundingStrategy(100));
        Runnable writer = () -> IntStream.range(0, 1000).forEach(buffer::add);
        Thread t1 = new Thread(writer);
        Thread t2 = new Thread(writer);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertThat(buffer.size()).isEqualTo(100); // never exceeds the bound
    }
}
