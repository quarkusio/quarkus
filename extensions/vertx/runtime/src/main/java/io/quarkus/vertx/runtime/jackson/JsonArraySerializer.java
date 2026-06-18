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

import io.vertx.core.json.JsonArray;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Copied from {@code io.vertx.core.json.jackson.JsonArraySerializer} as that class is package private
 */
public class JsonArraySerializer extends StdSerializer<JsonArray> {
    public JsonArraySerializer() {
        super(JsonArray.class);
    }

    public void serialize(JsonArray value, tools.jackson.core.JsonGenerator jgen, SerializationContext provider) {
        jgen.writePOJO(value.getList());
    }
}
