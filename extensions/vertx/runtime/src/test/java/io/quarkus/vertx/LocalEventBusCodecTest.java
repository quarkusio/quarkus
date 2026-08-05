package io.quarkus.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;

public class LocalEventBusCodecTest {

    @Test
    public void transformReturnsSameInstance() {
        LocalEventBusCodec<String> codec = new LocalEventBusCodec<>();
        String instance = "hello";
        assertSame(instance, codec.transform(instance));
    }

    @Test
    public void transformReturnsSameInstanceForComplexType() {
        LocalEventBusCodec<Object> codec = new LocalEventBusCodec<>();
        Object instance = new Object();
        assertSame(instance, codec.transform(instance));
    }

    @Test
    public void encodeToWireThrowsUnsupportedOperationException() {
        LocalEventBusCodec<String> codec = new LocalEventBusCodec<>();
        assertThatThrownBy(() -> codec.encodeToWire(Buffer.buffer(), "hello"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void decodeFromWireThrowsUnsupportedOperationException() {
        LocalEventBusCodec<String> codec = new LocalEventBusCodec<>();
        assertThatThrownBy(() -> codec.decodeFromWire(0, Buffer.buffer()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void systemCodecIDReturnsMinusOne() {
        LocalEventBusCodec<String> codec = new LocalEventBusCodec<>();
        assertThat(codec.systemCodecID()).isEqualTo((byte) -1);
    }

    @Test
    public void nameIsUniqueAcrossInstances() {
        LocalEventBusCodec<String> codec1 = new LocalEventBusCodec<>();
        LocalEventBusCodec<String> codec2 = new LocalEventBusCodec<>();
        assertThat(codec1.name()).isNotEqualTo(codec2.name());
    }

    @Test
    public void nameContainsClassName() {
        LocalEventBusCodec<String> codec = new LocalEventBusCodec<>();
        assertThat(codec.name()).startsWith(LocalEventBusCodec.class.getName());
    }

    @Test
    public void constructorWithExplicitName() {
        String customName = "my-custom-codec";
        LocalEventBusCodec<String> codec = new LocalEventBusCodec<>(customName);
        assertThat(codec.name()).isEqualTo(customName);
    }
}
