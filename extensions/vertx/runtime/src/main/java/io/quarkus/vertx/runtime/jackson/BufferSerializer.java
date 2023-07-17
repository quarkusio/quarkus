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

import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_ENCODER;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.vertx.core.buffer.Buffer;

/**
 * Copied from {@code io.vertx.core.json.jackson.BufferSerializer} as that class is package private
 */
class BufferSerializer extends JsonSerializer<Buffer> {

    @Override
    public void serialize(Buffer value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(BASE64_ENCODER.encodeToString(value.getBytes()));
    }
}
