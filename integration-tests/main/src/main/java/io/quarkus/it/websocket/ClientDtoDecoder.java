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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class ClientDtoDecoder implements Decoder.TextStream<Dto> {
    @Override
    public Dto decode(Reader reader) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String input = bufferedReader.readLine(); // expecting one line input
            Dto result = new Dto();
            result.setContent("[decoded]" + input);
            return result;
        }
    }

    @Override
    public void init(EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }
}
