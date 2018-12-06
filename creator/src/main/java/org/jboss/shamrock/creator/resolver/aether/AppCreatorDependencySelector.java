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
package org.jboss.shamrock.creator.resolver.aether;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.jboss.logging.Logger;

/**
 *
 * @author Alexey Loubyansky
 */
public class AppCreatorDependencySelector implements DependencySelector {

    private static final String COMPILE = "compile";
    private static final String PROVIDED = "provided";
    private static final String RUNTIME = "runtime";
    private static final String SYSTEM = "system";
    private static final String WILDCARD = "*";

    private static final Logger log = Logger.getLogger(AppCreatorDependencySelector.class);

    private static class DepExclusions {

        private final Collection<Exclusion> exclusions;
        private final DepExclusions parent;

        DepExclusions(DepExclusions parent, Collection<Exclusion> exclusions) {
            this.parent = parent;
            this.exclusions = exclusions;
        }

        boolean isExcluded(Dependency dep) {
            for(Exclusion excl : exclusions) {
                final String exclGroupId = excl.getGroupId();
                final String exclArtifactId = excl.getArtifactId();
                final String exclClassifier = excl.getClassifier();
                final String exclExt = excl.getExtension();
                final Artifact artifact = dep.getArtifact();
                if((exclGroupId.equals(WILDCARD) || exclGroupId.equals(artifact.getGroupId())) &&
                        (exclArtifactId.equals(WILDCARD) || exclArtifactId.equals(artifact.getArtifactId())) &&
                        (exclClassifier.equals(WILDCARD) || exclClassifier.equals(artifact.getClassifier())) &&
                        (exclExt.equals(WILDCARD) || exclExt.equals(artifact.getExtension()))) {
                    return true;
                }
            }
            if(parent != null) {
                DepExclusions parent = this.parent;
                while(parent != null) {
                    if(parent.isExcluded(dep)) {
                        return true;
                    }
                    parent = parent.parent;
                }
            }
            return false;
        }
    }

    private static final Set<String> APP_SCOPES = new HashSet<>(Arrays.asList(new String[] {COMPILE, SYSTEM, PROVIDED, RUNTIME}));
    private static final Set<String> TRANSITIVE_SCOPES = new HashSet<>(Arrays.asList(new String[] {COMPILE, SYSTEM, RUNTIME}));

    private final int depth;
    private final Set<String> selected;
    private final Set<String> childrenProcessed;
    private final Set<String> acceptedScopes;
    private Dependency lastSelected;
    private final DepExclusions depExclusions;
    //private Map<String, Collection<Exclusion>> managedExcl;

    public AppCreatorDependencySelector(boolean debug) {
        this.depth = debug ? 0 : -1;
        this.selected = new HashSet<>();
        this.childrenProcessed = new HashSet<>();
        this.acceptedScopes = APP_SCOPES;
        this.depExclusions = null;
    }

    private AppCreatorDependencySelector(AppCreatorDependencySelector parent, Set<String> acceptedScopes, DepExclusions depExclusions) {
        this.depth = parent.depth < 0 ? parent.depth : parent.depth + 1;
        this.selected = parent.selected;
        this.childrenProcessed = parent.childrenProcessed;
        //this.managedExcl = parent.managedExcl;
        this.acceptedScopes = acceptedScopes;
        this.depExclusions = depExclusions;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        if (dependency.isOptional() || !acceptedScopes.contains(dependency.getScope())
                || !selected.add(dependency.getArtifact().toString())
                || depExclusions != null && depExclusions.isExcluded(dependency)) {
            return false;
        }
        if(depth >= 0) {
            final StringBuilder buf = new StringBuilder();
            for (int i = 0; i < depth; ++i) {
                buf.append("  ");
            }
            String offset = buf.toString();
            log.debug(offset + dependency);
            if(!dependency.getExclusions().isEmpty()) {
                log.debug(offset + " Excludes:");
                buf.append("  - ");
                offset = buf.toString();
                for(Exclusion excl : dependency.getExclusions()) {
                    log.debug(offset + excl.getGroupId() + ":" + excl.getArtifactId() + ":" + excl.getClassifier() + ":" + excl.getExtension());
                }
            }
        }
        lastSelected = dependency;
        return true;
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        /* doesn't seem to be relevant to look into the managed deps at the root
        if(managedExcl == null && depth <= 0) {
            final List<Dependency> managedDeps = context.getManagedDependencies();
            if(managedDeps.isEmpty()) {
                managedExcl = Collections.emptyMap();
            } else {
                System.out.println("Managed dependencies");
                managedExcl = new HashMap<>(managedDeps.size());
                for(Dependency dep : managedDeps) {
                    final Collection<Exclusion> exclusions = dep.getExclusions();
                    if(!exclusions.isEmpty()) {
                        managedExcl.put(getKey(dep.getArtifact()), exclusions);
                        if(depth == 0) {
                            System.out.println("  " + dep);
                            System.out.println("    Excludes:");
                            for (Exclusion excl : exclusions) {
                                System.out.println("    - " + excl.getGroupId() + ":" + excl.getArtifactId() + ":"
                                        + excl.getClassifier() + ":" + excl.getExtension());
                            }
                        }
                    }
                }
            }
        }
        */
        final Dependency dependency = context.getDependency();
        if(lastSelected != null && !lastSelected.getArtifact().getVersion().equals(dependency.getArtifact().getVersion())) {
            return DisabledDependencySelector.INSTANCE;
        }
        if (dependency.isOptional() || !acceptedScopes.contains(dependency.getScope())
                || !childrenProcessed.add(dependency.getArtifact().toString())) {
            return DisabledDependencySelector.INSTANCE;
        }
        if(acceptedScopes.size() != TRANSITIVE_SCOPES.size() && dependency.getScope().equals(PROVIDED)) {
            return new AppCreatorDependencySelector(this, TRANSITIVE_SCOPES,
                    dependency.getExclusions().isEmpty() ? depExclusions : new DepExclusions(depExclusions, dependency.getExclusions()));
        }
        return depth < 0 && dependency.getExclusions().isEmpty() ? this : new AppCreatorDependencySelector(this, acceptedScopes,
                dependency.getExclusions().isEmpty() ? depExclusions : new DepExclusions(depExclusions, dependency.getExclusions()));
    }
/*
    private static String getKey(Artifact artifact) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(artifact.getGroupId());
        sb.append(':');
        sb.append(artifact.getArtifactId());
        sb.append(':');
        sb.append(artifact.getExtension());
        if (!artifact.getClassifier().isEmpty()) {
            sb.append(':');
            sb.append(artifact.getClassifier());
        }
        return sb.toString();
    }
    */
}