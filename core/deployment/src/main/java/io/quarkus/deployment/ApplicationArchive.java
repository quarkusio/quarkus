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
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.jandex.IndexView;

/**
 * Represents an archive that is part of application code.
 * <p>
 * An application archive is an archive that provides components to the application. As a result it will be indexed
 * via jandex, and any deployment descriptors will be made available to the application.
 */
public interface ApplicationArchive extends Closeable {

    /**
     *
     * @return The index of this application Archive
     */
    IndexView getIndex();

    /**
     *
     * Returns a path representing the archive root. Note that if this is a jar archive this is not the path to the
     * jar, but rather a path to the root of the mounted {@link com.sun.nio.zipfs.ZipFileSystem}
     *
     * @return The archive root.
     */
    Path getArchiveRoot();

    /**
     *
     * @return <code>true</code> if this archive is a jar
     */
    boolean isJarArchive();

    /**
     * If this archive is a jar file it will return the path to the jar file on the file system,
     * otherwise it will return the directory that this corresponds to.
     */
    Path getArchiveLocation();

    /**
     * Convenience method, returns the child path if it exists, otherwise null.
     *
     * @param path The child path
     * @return The child path, or null if it does not exist.
     */
    default Path getChildPath(String path) {
        Path result = getArchiveRoot().resolve(path);
        if (Files.exists(result)) {
            return result;
        }
        return null;
    }

}
