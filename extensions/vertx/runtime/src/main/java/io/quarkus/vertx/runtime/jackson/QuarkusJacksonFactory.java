package io.quarkus.vertx.runtime.jackson;

import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.spi.JsonFactory;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Tie into Vert.x's {@code JsonFactory} SPI in order to ensure that the user customized {@code ObjectMapper} is used
 */
public class QuarkusJacksonFactory implements JsonFactory {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Override
    public JsonCodec codec() {
        return Holder.JSON_CODEC;
    }

    public static void reset() {
        // if we blindly reset, we could get NCDFE because Jackson classes would not have been loaded
        if (COUNTER.get() > 0) {
            QuarkusJacksonJsonCodec.reset();
        }
    }

    // use this class to fool GraalVM's bytecode parser
    private static final class Holder {
        private static final JsonCodec JSON_CODEC = determineCodec();

        private static JsonCodec determineCodec() {
            JsonCodec codec;
            if (databindOnClassPath()) {
                codec = new QuarkusJacksonJsonCodec();
                COUNTER.incrementAndGet();
            } else {
                codec = JsonUtil.loadJacksonCodec();
            }
            return codec;

        }

        private static boolean databindOnClassPath() {
            try {
                Class.forName("tools.jackson.databind.ObjectMapper", false, Thread.currentThread().getContextClassLoader());
                return true;
            } catch (ClassNotFoundException ignored) {
                return false;
            }
        }
    }

}
