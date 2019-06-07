/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.it.websocket;

import java.io.Writer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class ServerDtoEncoder implements Encoder.TextStream<Dto> {
    @Override
    public void encode(Dto object, Writer writer) {
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("content", object.getContent())
                .build();
        Json.createWriter(writer)
                .writeObject(jsonObject);
    }

    @Override
    public void init(EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }
}
