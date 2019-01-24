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

package org.jboss.shamrock.creator.resolver.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;
import org.jboss.shamrock.creator.AppArtifact;

/**
 *
 * @author Alexey Loubyansky
 */
public class TsArtifact {

    static final String DEFAULT_GROUP_ID = "org.shamrock.creator.test";

    public static TsArtifact getGa(String groupId, String artifactId) {
        return new TsArtifact(groupId, artifactId, null);
    }

    public static TsArtifact getGa(String artifactId) {
        return getGa(DEFAULT_GROUP_ID, artifactId);
    }

    protected final String groupId;
    protected final String artifactId;
    protected final String classifier;
    protected final String type;
    protected final String version;

    private List<TsDependency> deps = Collections.emptyList();

    public TsArtifact(String artifactId) {
        this(artifactId, "1");
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
}
