package io.quarkus.vertx.core.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class DefaultEventLoopCountTest {

    private static final long MB = 1024 * 1024;

    @Test
    public void oneEventLoopPerCpu() {
        assertThat(VertxCoreRecorder.calculateDefaultIOThreads(1, 1024 * MB)).isEqualTo(1);
        assertThat(VertxCoreRecorder.calculateDefaultIOThreads(2, 1024 * MB)).isEqualTo(2);
        assertThat(VertxCoreRecorder.calculateDefaultIOThreads(8, 1024 * MB)).isEqualTo(8);
    }

    @Test
    public void oneEventLoopPerTenMegabytesOfHeap() {
        assertThat(VertxCoreRecorder.calculateDefaultIOThreads(8, 30 * MB)).isEqualTo(3);
    }

    @Test
    public void atLeastOneEventLoop() {
        assertThat(VertxCoreRecorder.calculateDefaultIOThreads(8, 5 * MB)).isEqualTo(1);
    }
}
