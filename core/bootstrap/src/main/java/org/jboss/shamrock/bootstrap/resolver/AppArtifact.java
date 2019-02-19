/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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

package org.jboss.shamrock.bootstrap.resolver;

import java.nio.file.Path;

/**
 * Represents an application (or its dependency) artifact.
 *
 * @author Alexey Loubyansky
 */
public class AppArtifact extends GACTV {

    private static final String TYPE_JAR = "jar";

    protected Path path;

    public AppArtifact(String groupId, String artifactId, String version) {
        this(groupId, artifactId, "", TYPE_JAR, version);
    }

    public AppArtifact(String groupId, String artifactId, String classifier, String version) {
        this(groupId, artifactId, classifier, TYPE_JAR, version);
    }

    public AppArtifact(String groupId, String artifactId, String classifier, String type, String version) {
        super(groupId, artifactId, classifier, type, version);
    }

    Path getPath() {
        return path;
    }

    protected void setPath(Path path) {
        this.path = path;
    }

    public boolean isResolved() {
        return path != null;
    }
}
