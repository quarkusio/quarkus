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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;

import io.quarkus.bootstrap.model.AppArtifact;

/**
 *
 * @author Alexey Loubyansky
 */
public class TsArtifact {

    public static final String DEFAULT_GROUP_ID = "org.quarkus.creator.test";
    public static final String DEFAULT_VERSION = "1";

    public static TsArtifact ga(String artifactId) {
        return ga(DEFAULT_GROUP_ID, artifactId);
    }

    public static TsArtifact ga(String groupId, String artifactId) {
        return new TsArtifact(groupId, artifactId, null);
    }

    public static TsArtifact jar(String artifactId) {
        return jar(artifactId, DEFAULT_VERSION);
    }

    public static TsArtifact jar(String artifactId, String version) {
        return new TsArtifact(DEFAULT_GROUP_ID, artifactId, "", "jar", version);
    }

    protected final String groupId;
    protected final String artifactId;
    protected final String classifier;
    protected final String type;
    protected final String version;

    private List<TsDependency> deps = Collections.emptyList();

    public TsArtifact(String artifactId) {
        this(artifactId, DEFAULT_VERSION);
    }

    public TsArtifact(String artifactId, String version) {
        this(DEFAULT_GROUP_ID, artifactId, "", "txt", version);
    }

    public TsArtifact(String groupId, String artifactId, String version) {
        this(groupId, artifactId, "", "txt", version);
    }

    public TsArtifact(String groupId, String artifactId, String classifier, String type, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.type = type;
        this.version = version;
    }

    public TsArtifact addDependency(TsArtifact dep) {
        return addDependency(new TsDependency(dep));
    }

    public TsArtifact addDependency(TsDependency dep) {
        if(deps.isEmpty()) {
            deps = new ArrayList<>();
        }
        deps.add(dep);
        return this;
    }

    public String getArtifactFileName() {
        if(artifactId == null) {
            throw new IllegalArgumentException("artifactId is missing");
        }
        if(version == null) {
            throw new IllegalArgumentException("version is missing");
        }
        if(type == null) {
            throw new IllegalArgumentException("type is missing");
        }
        final StringBuilder fileName = new StringBuilder();
        fileName.append(artifactId).append('-').append(version);
        if(classifier != null && !classifier.isEmpty()) {
            fileName.append('-').append(classifier);
        }
        fileName.append('.').append(type);
        return fileName.toString();
    }

    public TsArtifact toPomArtifact() {
        return new TsArtifact(groupId, artifactId, "", "pom", version);
    }

    public Model getPomModel() {
        final Model model = new Model();
        model.setModelVersion("4.0.0");

        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setPackaging(type);
        model.setVersion(version);

        if(!deps.isEmpty()) {
            for (TsDependency dep : deps) {
                model.addDependency(dep.toPomDependency());
            }
        }

        return model;
    }

    public AppArtifact toAppArtifact() {
        return new AppArtifact(groupId, artifactId, classifier, type, version);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder(128);
        buf.append(groupId).append(':').append(artifactId).append(':').append(classifier).append(':').append(type).append(':').append(version);
        return buf.toString();
    }
}
