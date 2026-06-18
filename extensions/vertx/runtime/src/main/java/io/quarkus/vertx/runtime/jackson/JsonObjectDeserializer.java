/*
 * Copyright (c) 2011-2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.quarkus.vertx.runtime.jackson;

import java.util.Map;

import io.vertx.core.json.JsonObject;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Copied from {@code io.vertx.core.json.jackson.JsonObjectDeserializer} as that class is package private
 */
public class JsonObjectDeserializer extends StdDeserializer<JsonObject> {

    private static final TypeReference<Map<String, Object>> TYPE_REF = new TypeReference<>() {
    };

    public JsonObjectDeserializer() {
        super(JsonObject.class);
    }

    @Override
    public JsonObject deserialize(JsonParser p, DeserializationContext ctxt) {
        return new JsonObject(p.<Map<String, Object>> readValueAs(TYPE_REF));
    }
}
