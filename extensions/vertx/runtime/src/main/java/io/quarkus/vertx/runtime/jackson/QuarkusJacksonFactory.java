package io.quarkus.vertx.runtime.jackson;

import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.json.jackson.JacksonCodec;
import io.vertx.core.spi.JsonFactory;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Tie into Vert.x's {@code JsonFactory} SPI in order to ensure that the user customized {@code ObjectMapper} is used
 */
public class QuarkusJacksonFactory implements JsonFactory {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Override
    public JsonCodec codec() {
        JsonCodec codec;
        try {
            // First try the Quarkus databind codec
            codec = new QuarkusJacksonJsonCodec();
            COUNTER.incrementAndGet();
        } catch (Throwable t1) {
            // Then try the Vert.x databind codec
            try {
                codec = new DatabindCodec();
            } catch (Throwable t2) {
                // Finally, use the jackson.core codec
                codec = new JacksonCodec();
            }
        }
        return codec;
    }

    public static void reset() {
        // if we blindly reset, we could get NCDFE because Jackson classes would not have been loaded
        if (COUNTER.get() > 0) {
            QuarkusJacksonJsonCodec.reset();
        }
    }

}
