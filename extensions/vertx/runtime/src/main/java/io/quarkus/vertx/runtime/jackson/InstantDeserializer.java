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

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * Copied from {@code io.vertx.core.json.jackson.InstantDeserializer} as that class is package private
 */
class InstantDeserializer extends JsonDeserializer<Instant> {
    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String text = p.getText();
        try {
            return Instant.from(ISO_INSTANT.parse(text));
        } catch (DateTimeException e) {
            throw new InvalidFormatException(p, "Expected an ISO 8601 formatted date time", text, Instant.class);
        }
    }
}
