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

import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

import io.vertx.core.buffer.Buffer;

/**
 * Copied from {@code io.vertx.core.json.jackson.BufferDeserializer} as that class is package private
 */
public class BufferDeserializer extends ValueDeserializer<Buffer> {

    @Override
    public Buffer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        String text = p.getText();
        try {
            return Buffer.buffer(BASE64_DECODER.decode(text));
        } catch (IllegalArgumentException e) {
            throw new InvalidFormatException(p, "Expected a base64 encoded byte array", text, Buffer.class);
        }
    }
}
