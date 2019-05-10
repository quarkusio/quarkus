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

import java.nio.file.Path;

import io.quarkus.bootstrap.model.AppArtifact;

/**
 * Defines the build context of a local project.
 */
public interface LocalProject {
    /**
     * Returns builder's output directory.
     * 
     * This is used for storing cached classpath information.
     * 
     * @return builder's output directory.
     */
    Path getOutputDir();
    LocalWorkspace getWorkspace();
    AppArtifact getAppArtifact();
    String getGroupId();
    String getArtifactId();
    String getVersion();
}
