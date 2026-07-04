package org.jboss.resteasy.reactive.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

class MediaTypeHelperTest {

    @Test
    void parseHeaderReturnsEmptyListForEntirelyInvalidHeader() {
        assertTrue(MediaTypeHelper.parseHeader("/").isEmpty());
        assertTrue(MediaTypeHelper.parseHeader("invalid").isEmpty());
    }

    @Test
    void parseHeaderSkipsInvalidTokens() {
        List<MediaType> types = MediaTypeHelper.parseHeader("text/plain, /, application/json");
        assertEquals(2, types.size());
        assertTrue(types.get(0).isCompatible(MediaType.TEXT_PLAIN_TYPE));
        assertTrue(types.get(1).isCompatible(MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    void parseHeaderParsesValidHeader() {
        List<MediaType> types = MediaTypeHelper.parseHeader("text/plain, application/json;q=0.8");
        assertEquals(2, types.size());
        assertTrue(types.get(0).isCompatible(MediaType.TEXT_PLAIN_TYPE));
        assertTrue(types.get(1).isCompatible(MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    void negotiateProducesDoesNotThrowForInvalidAcceptHeader() {
        ServerMediaType serverMediaType = new ServerMediaType(List.of(MediaType.APPLICATION_JSON_TYPE), null, false);
        Map.Entry<MediaType, MediaType> negotiated = serverMediaType.negotiateProduces("/");
        assertTrue(negotiated.getKey().isCompatible(MediaType.APPLICATION_JSON_TYPE));
    }
}
