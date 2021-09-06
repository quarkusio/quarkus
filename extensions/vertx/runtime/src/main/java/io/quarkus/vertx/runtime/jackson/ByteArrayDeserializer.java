/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.quarkus.vertx.runtime.jackson;

import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_DECODER;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * Copied from {@code io.vertx.core.json.jackson.ByteArrayDeserializer} as that class is package private
 */
class ByteArrayDeserializer extends JsonDeserializer<byte[]> {

    @Override
    public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String text = p.getText();
        try {
            return BASE64_DECODER.decode(text);
        } catch (IllegalArgumentException e) {
            throw new InvalidFormatException(p, "Expected a base64 encoded byte array", text, Instant.class);
        }
    }
}
