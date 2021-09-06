package io.quarkus.vertx.runtime.jackson;

import io.vertx.core.spi.JsonFactory;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Tie into Vert.x's {@code JsonFactory} SPI in order to ensure that the user customized {@code ObjectMapper} is used
 */
public class QuarkusJacksonFactory implements JsonFactory {

    @Override
    public JsonCodec codec() {
        return new QuarkusJacksonJsonCodec();
    }

}
