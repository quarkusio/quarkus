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

package org.jboss.shamrock.creator.resolver.aether;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.transformer.ConflictIdSorter;
import org.eclipse.aether.util.graph.transformer.ConflictMarker;
import org.jboss.logging.Logger;
import org.jboss.shamrock.bootstrap.BootstrapDependencyProcessingException;
import org.jboss.shamrock.bootstrap.BootstrapDependencyProcessor;

/**
 *
 * @author Alexey Loubyansky
 */
public class BootstrapDependencyGraphTransformer implements DependencyGraphTransformer {

    private static final Logger log = Logger.getLogger(BootstrapDependencyGraphTransformer.class);

    private final RepositorySystem system;
    private final DependencyGraphTransformer delegate;
    private final BootstrapDependencyProcessor appDepProcessor;
    private final ConflictMarker conflictMarker;
    private final ConflictIdSorter conflictIdSorter;
    private boolean injectDeps = true;
    private Set<String> changeTriggers = new HashSet<>(1);

    public BootstrapDependencyGraphTransformer(RepositorySystem system, DependencyGraphTransformer delegate) {
        this.system = system;
        this.delegate = delegate;
        this.appDepProcessor = new BootstrapDependencyProcessor();
        this.conflictMarker = new ConflictMarker();
        this.conflictIdSorter = new ConflictIdSorter();
    }

    @Override
    public DependencyNode transformGraph(DependencyNode node, DependencyGraphTransformationContext context)
            throws RepositoryException {
        node = delegate.transformGraph(node, context);
        if(!injectDeps) {
            return node;
        }
        //System.out.println("ORIGINAL GRAPH for " + node.getArtifact());
        //log(node);

        injectDeps = false;
        try {
            final AetherBootstrapDependencyProcessingContext ctx = new AetherBootstrapDependencyProcessingContext(system, context.getSession());
            if(!injectDeps(node, ctx)) {
                return node;
            }
        } finally {
            injectDeps = true;
        }

        //System.out.println("AUGMENTED GRAPH");
        //log(node);

        // add conflict IDs to the added deployments
        node = conflictMarker.transformGraph(node, context);
        // resolves version conflicts
        node = conflictIdSorter.transformGraph(node, context);

        node = delegate.transformGraph(node, context);
        log("Final dependency graph", node);
        return node;
    }

    private boolean injectDeps(DependencyNode node, AetherBootstrapDependencyProcessingContext ctx) throws RepositoryException {
        if(changeTriggers.contains(getKey(node))) {
            return true;
        }
        ctx.setDependency(node);
        try {
            appDepProcessor.process(ctx);
            if (ctx.isReprocess()) {
                final String key = getKey(node);
                changeTriggers.add(key);
                try {
                    injectDeps(node, ctx);
                } finally {
                    changeTriggers.remove(key);
                }
                return true;
            }
        } catch (BootstrapDependencyProcessingException e) {
            throw new RepositoryException("Failed to process dependency " + node.getDependency(), e);
        }
        final List<DependencyNode> children = node.getChildren();
        if (children.isEmpty()) {
            return ctx.isUpdated();
        }

        boolean injected = false;
        String key = null;
        if(ctx.isUpdated()) {
            key = getKey(node);
            changeTriggers.add(key);
            injected = true;
        }
        try {
            for (DependencyNode child : children) {
                injected |= injectDeps(child, ctx);
            }
        } finally {
            if(key != null) {
                changeTriggers.remove(key);
            }
        }
        return injected;
    }

    private static String getKey(DependencyNode node) {
        final Artifact art = node.getArtifact();
        final StringBuilder buf = new StringBuilder(128);
        buf.append(art.getGroupId()).append(':').append(':').append(art.getArtifactId()).append(':');
        final String classifier = art.getClassifier();
        if(classifier != null && !classifier.isEmpty()) {
            buf.append(classifier).append(':');
        }
        return buf.append(art.getExtension()).append(':').append(art.getVersion()).toString();
    }

    public static void log(String header, DependencyNode node) {
        log.info(header + " for " + node.getArtifact());
        log(node, 0);
    }
    private static void log(DependencyNode node, int depth) {
        final StringBuilder buf = new StringBuilder();
        for(int i = 0; i < depth; ++i) {
            buf.append("  ");
        }
        buf.append(node.getArtifact());
        buf.append('(').append(node.getDependency().getScope()).append(')');
        //log.info(buf);
        System.out.println(buf);
        final List<DependencyNode> children = node.getChildren();
        if(children.isEmpty()) {
            return;
        }
        for(DependencyNode child : children) {
            log(child, depth + 1);
        }
    }
}
