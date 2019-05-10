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

import java.util.Map;

import io.quarkus.bootstrap.model.AppArtifactKey;

/**
 * Defines a local development workspace.
 */
public interface LocalWorkspace {
    /**
     * Returns an ID representing the current build configuration.
     * @return an ID representing the current build configuration.
     */
    int getId();

    /**
     * Returns the projects in the workspace, indexed by GAV.
     * @return the projects in the workspace, indexed by GAV.
     */
    Map<AppArtifactKey, LocalProject> getProjects();
}
