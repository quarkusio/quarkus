/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.quarkus.container.image.jib.deployment;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

import com.google.cloud.tools.jib.api.JavaContainerBuilder;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;
import com.google.cloud.tools.jib.filesystem.DirectoryWalker;

/**
 * Copied almost verbatim from Jib's {@code com.google.cloud.tools.jib.plugins.common.JavaContainerBuilderHelper}
 * because the module that contains it is internal to Jib
 */
final class ContainerBuilderHelper {

    private ContainerBuilderHelper() {
    }

    /**
     * Returns a {@link FileEntriesLayer} for adding the extra directory to the container.
     *
     * @param sourceDirectory the source extra directory path
     * @param targetDirectory the root directory on the container to place the files in
     * @param extraDirectoryPermissions map from path on container to file permissions
     * @param modificationTimeProvider file modification time provider
     * @return a {@link FileEntriesLayer} for adding the extra directory to the container
     * @throws IOException if walking the extra directory fails
     */
    public static FileEntriesLayer extraDirectoryLayerConfiguration(
            Path sourceDirectory,
            AbsoluteUnixPath targetDirectory,
            Map<String, FilePermissions> extraDirectoryPermissions,
            BiFunction<Path, AbsoluteUnixPath, Instant> modificationTimeProvider)
            throws IOException {
        FileEntriesLayer.Builder builder = FileEntriesLayer.builder()
                .setName(JavaContainerBuilder.LayerType.EXTRA_FILES.getName());
        Map<PathMatcher, FilePermissions> pathMatchers = new LinkedHashMap<>();
        for (Map.Entry<String, FilePermissions> entry : extraDirectoryPermissions.entrySet()) {
            pathMatchers.put(
                    FileSystems.getDefault().getPathMatcher("glob:" + entry.getKey()), entry.getValue());
        }

        new DirectoryWalker(sourceDirectory)
                .filterRoot()
                .walk(
                        localPath -> {
                            AbsoluteUnixPath pathOnContainer = targetDirectory.resolve(sourceDirectory.relativize(localPath));
                            Instant modificationTime = modificationTimeProvider.apply(localPath, pathOnContainer);
                            FilePermissions permissions = extraDirectoryPermissions.get(pathOnContainer.toString());
                            if (permissions == null) {
                                // Check for matching globs
                                Path containerPath = Paths.get(pathOnContainer.toString());
                                for (Map.Entry<PathMatcher, FilePermissions> entry : pathMatchers.entrySet()) {
                                    if (entry.getKey().matches(containerPath)) {
                                        builder.addEntry(
                                                localPath, pathOnContainer, entry.getValue(), modificationTime);
                                        return;
                                    }
                                }

                                // Add with default permissions
                                if (localPath.toFile().canExecute()) {
                                    // make sure the file or directory can be executed
                                    builder.addEntry(localPath, pathOnContainer, FilePermissions.fromOctalString("755"),
                                            modificationTime);
                                } else {
                                    builder.addEntry(localPath, pathOnContainer, modificationTime);
                                }
                            } else {
                                // Add with explicit permissions
                                builder.addEntry(localPath, pathOnContainer, permissions, modificationTime);
                            }
                        });
        return builder.build();
    }
}
