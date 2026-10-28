package io.quarkus.it.netty;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class NettySizingTest {

    // Netty's defaults, see MultithreadEventLoopGroup and AdaptivePoolingAllocator
    private static final long LOW_MEMORY_THRESHOLD = 512 * 1024 * 1024;
    private static final int DEFAULT_MAGAZINE_BUFFER_QUEUE_CAPACITY = 1024;

    @Test
    @Launch
    public void nettyIsSizedFromTheRuntimeEnvironment(LaunchResult result) {
        Map<String, String> values = values(result);
        int availableProcessors = Integer.parseInt(values.get("runtime.availableProcessors"));
        long maxMemory = Long.parseLong(values.get("runtime.maxMemory"));
        String magazineBufferQueueCapacity = values.get("runtime.magazineBufferQueueCapacity");

        Map<String, String> expected = new HashMap<>();
        expected.put("netty.availableProcessors", String.valueOf(availableProcessors));
        expected.put("netty.mainEventLoopGroup.executors", String.valueOf(availableProcessors * 2));
        expected.put("netty.defaultSizedEventLoopGroup.executors", String.valueOf(availableProcessors * 2));
        expected.put("netty.allocator", "io.netty.buffer.AdaptiveByteBufAllocator");
        expected.put("netty.adaptive.lowMemory", String.valueOf(maxMemory <= LOW_MEMORY_THRESHOLD));
        expected.put("netty.adaptive.chunkReuseQueueCapacity", String.valueOf(Math.max(2, availableProcessors * 2)));
        expected.put("netty.adaptive.magazineBufferQueueCapacity", "null".equals(magazineBufferQueueCapacity)
                ? String.valueOf(DEFAULT_MAGAZINE_BUFFER_QUEUE_CAPACITY)
                : magazineBufferQueueCapacity);
        // Compare all the entries at once to report every mismatch
        assertThat(values).containsAllEntriesOf(expected);
    }

    static Map<String, String> values(LaunchResult result) {
        Map<String, String> values = new HashMap<>();
        for (String line : result.getOutputStream()) {
            int separator = line.indexOf('=');
            if (separator > 0 && (line.startsWith("runtime.") || line.startsWith("netty."))) {
                values.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
        return values;
    }
}
