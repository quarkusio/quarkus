/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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

package io.quarkus.deployment.dev.filewatch;

import java.io.Closeable;
import java.io.File;

/**
 * File system watcher service. This watcher can be used to receive notifications about a specific path.
 *
 * @author Stuart Douglas
 */
public interface FileSystemWatcher extends Closeable {

    /**
     * Watch the given path recursively, and invoke the callback when a change is made.
     *
     * @param file The path to watch
     * @param callback The callback
     */
    void watchPath(final File file, final FileChangeCallback callback);

    /**
     * Stop watching a path.
     *
     * @param file the path
     * @param callback the callback
     */
    void unwatchPath(final File file, final FileChangeCallback callback);
}
