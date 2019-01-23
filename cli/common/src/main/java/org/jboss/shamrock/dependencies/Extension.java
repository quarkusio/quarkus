/*
 *
 *   Copyright (c) 2016-2018 Red Hat, Inc.
 *
 *   Red Hat licenses this file to you under the Apache License, version
 *   2.0 (the "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *   implied.  See the License for the specific language governing
 *   permissions and limitations under the License.
 */
package org.jboss.shamrock.dependencies;

import org.apache.maven.model.Dependency;
import org.jboss.shamrock.maven.utilities.MojoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Extension {

    private String artifactId;
    private String groupId;
    private String scope;
    private String version = MojoUtils.SHAMROCK_VERSION_VARIABLE;

    private String type;
    private String classifier;

    private String name;
    private String[] labels;

    public Extension() {
        // Use by mapper.
    }


    public String getArtifactId() {
        return artifactId;
    }

    public Extension setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public String getGroupId() {
        return groupId;
    }

    public Extension setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public Extension setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Extension setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getType() {
        return type;
    }

    public Extension setType(String type) {
        this.type = type;
        return this;
    }

    public String getClassifier() {
        return classifier;
    }

    public Extension setClassifier(String classifier) {
        this.classifier = classifier;
        return this;
    }

    public String[] getLabels() {
        return labels;
    }

    public Extension setLabels(String[] labels) {
        this.labels = labels;
        return this;
    }

    public String getName() {
        return name;
    }

    public Extension setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> labels() {
        List<String> list = new ArrayList<>();
        if (labels != null) {
            list.addAll(Stream.of(labels).map(String::toLowerCase).collect(Collectors.toList()));
        }
        list.add(artifactId.toLowerCase());
        return list;
    }

    public Dependency toDependency(boolean stripVersion) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        if (scope != null && !scope.isEmpty()) {
            dependency.setScope(scope);
        }
        if (classifier != null && !classifier.isEmpty()) {
            dependency.setClassifier(classifier);
        }
        if (version != null  && ! version.isEmpty()  && ! stripVersion) {
            dependency.setVersion(version);
        }
        return dependency;
    }

    public String toCoordinates() {
        return getGroupId() + ":" + getArtifactId();
    }
}

