package io.quarkus.kafka.client.runtime.dev.ui.model.decoder;

import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

class AvroDecoderTest {

    @Test
    void testCanDecodeWithMessageStarting0x00AndOver5length() {
        AvroDecoder decoder = new AvroDecoder((topic, data) -> null);

        Assert.assertTrue(decoder.canDecode(new byte[10]));
    }

    @Test
    void testCanDecodeWithMessageStarting0x00AndUnder5length() {
        AvroDecoder decoder = new AvroDecoder((topic, data) -> null);

        Assert.assertFalse(decoder.canDecode(new byte[2]));
    }

    @Test
    void testCanDecodeWithMessageStarting0x01AndOver5length() {
        AvroDecoder decoder = new AvroDecoder((topic, data) -> null);

        Assert.assertFalse(decoder.canDecode(new byte[] { 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 }));
    }

    @Test
    void testCanDecodeWithMessageNull() {
        AvroDecoder decoder = new AvroDecoder((topic, data) -> null);

        Assert.assertFalse(decoder.canDecode(null));
    }
}
