/*
 * Copyright 2018 Red Hat, Inc.
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

package io.quarkus.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

import org.jboss.jandex.IndexView;

import io.quarkus.builder.item.MultiBuildItem;

public final class ApplicationArchiveImpl extends MultiBuildItem implements ApplicationArchive, Closeable {

    private final IndexView indexView;
    private final Path archiveRoot;
    private final Closeable closeable;
    private final boolean jar;
    private final Path archiveLocation;

    public ApplicationArchiveImpl(IndexView indexView, Path archiveRoot, Closeable closeable, boolean jar,
            Path archiveLocation) {
        this.indexView = indexView;
        this.archiveRoot = archiveRoot;
        this.closeable = closeable;
        this.jar = jar;
        this.archiveLocation = archiveLocation;
    }

    @Override
    public IndexView getIndex() {
        return indexView;
    }

    @Override
    public Path getArchiveRoot() {
        return archiveRoot;
    }

    @Override
    public boolean isJarArchive() {
        return jar;
    }

    @Override
    public Path getArchiveLocation() {
        return archiveLocation;
    }

    @Override
    public void close() throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }
}
