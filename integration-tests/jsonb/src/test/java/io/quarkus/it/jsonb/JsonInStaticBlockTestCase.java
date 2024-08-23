package io.quarkus.it.jsonb;

import jakarta.json.bind.spi.JsonbProvider;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class JsonInStaticBlockTestCase {
    static {
        JsonbProvider.provider().create();
    }

    @Test
    void get() {

    }
}
