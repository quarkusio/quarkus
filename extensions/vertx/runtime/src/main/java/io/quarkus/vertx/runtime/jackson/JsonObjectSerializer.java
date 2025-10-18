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

import java.io.IOException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import io.vertx.core.json.JsonObject;

/**
 * Copied from {@code io.vertx.core.json.jackson.JsonObjectSerializer} as that class is package private
 */
public class JsonObjectSerializer extends ValueSerializer<JsonObject> {
    @Override
    public void serialize(JsonObject value, JsonGenerator jgen, SerializationContext provider) throws IOException {
        jgen.writeObject(value.getMap());
    }
}
