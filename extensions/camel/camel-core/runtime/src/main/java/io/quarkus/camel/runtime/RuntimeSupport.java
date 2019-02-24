/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.camel.runtime;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.support.IntrospectionSupport;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RuntimeSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeSupport.class);

    private RuntimeSupport() {
    }

    public static void loadConfigSources(Properties properties, String conf, String confd) {
        // Main location
        if (ObjectHelper.isNotEmpty(conf)) {
            try (Reader reader = Files.newBufferedReader(Paths.get(conf))) {
                properties.load(reader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Additional locations
        if (ObjectHelper.isNotEmpty(confd)) {
            Path root = Paths.get(confd);
            if (Files.exists(root)) {
                try {
                    List<Path> paths = Files.walk(root)
                            .filter(p -> p.getFileSystem().toString().endsWith(".properties"))
                            .collect(Collectors.toList());
                    for (Path path : paths) {
                        try (Reader reader = Files.newBufferedReader(path)) {
                            properties.load(reader);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void bindProperties(CamelContext context, Properties properties, Object target, String prefix) {
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }

        final String p = prefix;

        properties.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof String)
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> ((String) entry.getKey()).startsWith(p))
                .forEach(entry -> {
                    final String key = ((String) entry.getKey()).substring(p.length());
                    final Object val = entry.getValue();

                    try {
                        IntrospectionSupport.setProperty(context, target, key, val);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
    }

    public static void bindProperties(Properties properties, Object target, String prefix) {
        bindProperties(null, properties, target, prefix);
    }

}
