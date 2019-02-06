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

package org.jboss.shamrock.bootstrap;

import java.nio.file.Path;

/**
 *
 * @author Alexey Loubyansky
 */
public interface BootstrapDependencyProcessingContext {

    String getGroupId();

    String getArtifactId();

    String getClassifier();

    String getType();

    String getVersion();

    Path getPath() throws BootstrapDependencyProcessingException;

    void overrideVersion(String version);

    default void injectDependency(String groupId, String artifactId, String version) throws BootstrapDependencyProcessingException {
        injectDependency(groupId, artifactId, "", "jar", version);
    }

    void injectDependency(String groupId, String artifactId, String classifier, String type, String version) throws BootstrapDependencyProcessingException;
}
