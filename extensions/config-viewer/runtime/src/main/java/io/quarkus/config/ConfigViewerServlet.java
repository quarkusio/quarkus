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
package io.quarkus.config;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.Config;

/**
 * Servlet, which returns all config sources and properties as JSON. The config sources are sorted descending by ordinal,
 * the properties by name. If no config is defined an empty JSON object is returned.
 *
 * <p>
 * A typical output might look like:
 * </p>
 *
 * <pre>
 * {
 *   "sources": [
 *     {
 *       "source": "SysPropConfigSource",
 *       "ordinal": 400,
 *       "properties": {
 *         "file.encoding": "UTF-8",
 *         "file.separator": "/"
 *       }
 *     },
 *     {
 *       "source": "EnvConfigSource",
 *       "ordinal": 300,
 *       "properties": {
 *         "EDITOR": "vim",
 *         "LC_ALL": "en_US.UTF-8"
 *       }
 *     },
 *     {
 *       "source": "PropertiesConfigSource[source=resource:META-INF/microprofile-config.properties]",
 *       "ordinal": 100,
 *       "properties": {
 *         "greeting.message": "hello",
 *         "greeting.name": "quarkus"
 *       }
 *     }
 *   ]
 * }
 * </pre>
 */
@WebServlet
public class ConfigViewerServlet extends HttpServlet {

    private static final Map<String, ?> JSON_CONFIG = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);

    @Inject
    Config config;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        JsonBuilderFactory builderFactory = Json.createBuilderFactory(JSON_CONFIG);
        JsonWriterFactory writerFactory = Json.createWriterFactory(JSON_CONFIG);
        JsonWriter writer = writerFactory.createWriter(resp.getWriter());
        JsonObject json = new ConfigViewer().dump(config, builderFactory);
        writer.write(json);
    }
}
