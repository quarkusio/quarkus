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

package io.quarkus.creator.resolver.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;

/**
 *
 * @author Alexey Loubyansky
 */
public class TsDependency {

    protected final TsArtifact artifact;
    protected String scope;
    protected boolean optional;
    protected List<TsArtifact> excluded = Collections.emptyList();

    public TsDependency(TsArtifact artifact) {
        this(artifact, null, false);
    }

    public TsDependency(TsArtifact artifact, String scope) {
        this(artifact, scope, false);
    }

    public TsDependency(TsArtifact artifact, boolean optional) {
        this(artifact, null, optional);
    }

    public TsDependency(TsArtifact artifact, String scope, boolean optional) {
        this.artifact = artifact;
        this.scope = scope;
        this.optional = optional;
    }

    public TsDependency exclude(String artifactId) {
        return exclude(TsArtifact.getGa(artifactId));
    }

    public TsDependency exclude(String groupId, String artifactId) {
        return exclude(TsArtifact.getGa(groupId, artifactId));
    }

    public TsDependency exclude(TsArtifact artifact) {
        if (excluded.isEmpty()) {
            excluded = new ArrayList<>();
        }
        excluded.add(artifact);
        return this;
    }

    public Dependency toPomDependency() {
        final Dependency dep = new Dependency();
        dep.setGroupId(artifact.groupId);
        dep.setArtifactId(artifact.artifactId);
        final String updateClassifier = artifact.classifier;
        if (updateClassifier != null && !updateClassifier.isEmpty()) {
            dep.setClassifier(updateClassifier);
        }
        dep.setType(artifact.type);
        dep.setVersion(artifact.version);
        if (scope != null) {
            dep.setScope(scope);
        }
        if (optional) {
            dep.setOptional(optional);
        }
        if (!excluded.isEmpty()) {
            for (TsArtifact excluded : excluded) {
                final Exclusion exclusion = new Exclusion();
                exclusion.setGroupId(excluded.groupId);
                exclusion.setArtifactId(excluded.artifactId);
                dep.addExclusion(exclusion);
            }
        }
        return dep;
    }
}
