package io.quarkus.vertx.http.runtime.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.quarkus.vertx.core.runtime.VertxMDC;

class VertxMDCDataAttributeTest {

    @Test
    void returnsCorrectValue() {
        VertxMDC mdc = VertxMDC.INSTANCE;
        mdc.put("traceId", "123");

        VertxMDCDataAttribute mdcDataAttribute = new VertxMDCDataAttribute("traceId");
        assertThat(mdcDataAttribute.readAttribute(null)).isEqualTo("123");
    }

    @Test
    void returnsNullOnKeyNotInMDC() {
        VertxMDC mdc = VertxMDC.INSTANCE;
        mdc.put("traceId", "123");

        VertxMDCDataAttribute mdcDataAttribute = new VertxMDCDataAttribute("spanId");
        assertNull(mdcDataAttribute.readAttribute(null));
    }
}
