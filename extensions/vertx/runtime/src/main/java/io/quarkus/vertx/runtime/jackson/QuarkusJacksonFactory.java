package io.quarkus.vertx.runtime.jackson;

import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.json.jackson.JacksonCodec;
import io.vertx.core.spi.JsonFactory;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Tie into Vert.x's {@code JsonFactory} SPI in order to ensure that the user customized {@code ObjectMapper} is used
 */
public class QuarkusJacksonFactory implements JsonFactory {

    @Override
    public JsonCodec codec() {
        JsonCodec codec;
        try {
            // First try the Quarkus databind codec
            codec = new QuarkusJacksonJsonCodec();
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

}
