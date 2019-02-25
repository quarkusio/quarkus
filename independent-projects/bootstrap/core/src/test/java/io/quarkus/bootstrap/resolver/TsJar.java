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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import io.quarkus.bootstrap.util.ZipUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class TsJar {

    private final Path target;

    public TsJar(Path target) {
        this.target = target;
    }

    public Path getPath() {
        return target;
    }

    public TsJar addFile(String content, String... path) throws IOException {
        try (FileSystem zip = openZip()) {
            try (BufferedWriter writer = Files.newBufferedWriter(getPath(zip, path))) {
                writer.write(content);
            }
        } catch (Throwable t) {
            throw t;
        }
        return this;
    }

    public TsJar addFile(Properties props, String... path) throws IOException {
        try (FileSystem zip = openZip()) {
            final Path p = getPath(zip, path);
            Files.createDirectories(p.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(p)) {
                props.store(writer, "Written by TsJarBuilder");
            }
        } catch (Throwable t) {
            throw t;
        }
        return this;
    }

    private Path getPath(FileSystem zip, String... path) {
        Path p = zip.getPath(path[0]);
        if(path.length > 1) {
            for(int i = 1; i < path.length; ++i) {
                p = p.resolve(path[i]);
            }
        }
        return p;
    }

    private FileSystem openZip() throws IOException {
        if(Files.exists(target)) {
            return ZipUtils.newFileSystem(target);
        }
        Files.createDirectories(target.getParent());
        return ZipUtils.newZip(target);
    }
}
