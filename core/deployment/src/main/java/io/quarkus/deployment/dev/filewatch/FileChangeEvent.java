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

import java.io.File;

/**
 * The event object that is fired when a file system change is detected.
 *
 * @see FileSystemWatcher
 *
 * @author Stuart Douglas
 */
public class FileChangeEvent {

    private final File file;
    private final Type type;

    /**
     * Construct a new instance.
     *
     * @param file the file which is being watched
     * @param type the type of event that was encountered
     */
    public FileChangeEvent(File file, Type type) {
        this.file = file;
        this.type = type;
    }

    /**
     * Get the file which was being watched.
     *
     * @return the file which was being watched
     */
    public File getFile() {
        return file;
    }

    /**
     * Get the type of event.
     *
     * @return the type of event
     */
    public Type getType() {
        return type;
    }

    /**
     * Watched file event types. More may be added in the future.
     */
    public static enum Type {
        /**
         * A file was added in a directory.
         */
        ADDED,
        /**
         * A file was removed from a directory.
         */
        REMOVED,
        /**
         * A file was modified in a directory.
         */
        MODIFIED,
    }

}
