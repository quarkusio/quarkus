/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.bootstrap.resolver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import io.quarkus.bootstrap.util.ZipUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class TsJar implements TsArtifact.ContentProvider {

    private Path target;
    private Map<String, String> content = Collections.emptyMap();

    public TsJar(Path target) {
        this.target = target;
    }

    public TsJar() {
    }

    @Override
    public Path getPath(Path workDir) throws IOException {
        if (target == null) {
            target = workDir.resolve(UUID.randomUUID().toString());
        } else if(Files.exists(target)) {
            return target;
        }
        try (FileSystem zip = openZip()) {
            if (!content.isEmpty()) {
                for (Map.Entry<String, String> entry : content.entrySet()) {
                    final Path p = zip.getPath(entry.getKey());
                    Files.createDirectories(p.getParent());
                    try (BufferedWriter writer = Files.newBufferedWriter(p)) {
                        writer.write(entry.getValue());
                    }
                }
            }
        } catch (Throwable t) {
            throw t;
        }
        return target;
    }

    private String getKey(String... path) {
        if(path.length == 1) {
            return path[0];
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(path[0]);
        for(int i = 1; i < path.length; ++i) {
            buf.append('/').append(path[i]);
        }
        return buf.toString();
    }

    private void addContent(String content, String... path) {
        if(this.content.isEmpty()) {
            this.content = new HashMap<>(1);
        }
        this.content.put(getKey(path), content);
    }

    public TsJar addFile(String content, String... path) throws IOException {
        addContent(content, path);
        return this;
    }

    public TsJar addFile(Properties props, String... path) {
        final StringWriter writer = new StringWriter();
        try {
            props.store(writer, "Written by TsJarBuilder");
        } catch(IOException e) {
            throw new IllegalStateException("Failed to serialize properties", e);
        }
        addContent(writer.getBuffer().toString(), path);
        return this;
    }

    private FileSystem openZip() throws IOException {
        if(Files.exists(target)) {
            return ZipUtils.newFileSystem(target);
        }
        Files.createDirectories(target.getParent());
        return ZipUtils.newZip(target);
    }
}
