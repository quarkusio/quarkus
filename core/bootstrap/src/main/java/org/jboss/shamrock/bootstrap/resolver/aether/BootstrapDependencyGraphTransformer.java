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

package org.jboss.shamrock.bootstrap.resolver.aether;

import java.util.List;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.Dependency;
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
        //log("Original dependency graph", node);

        injectDeps = false;
        try {
            final AetherBootstrapDependencyProcessingContext ctx = new AetherBootstrapDependencyProcessingContext(system, context.getSession());
            if(!injectDeps(node, ctx)) {
                return node;
            }
        } finally {
            injectDeps = true;
        }

        //log("Augmented dependency graph", node);

        // add conflict IDs to the added deployments
        node = conflictMarker.transformGraph(node, context);
        // resolves version conflicts
        node = conflictIdSorter.transformGraph(node, context);

        node = delegate.transformGraph(node, context);
        //log("Final dependency graph", node);
        return node;
    }

    private boolean injectDeps(DependencyNode node, AetherBootstrapDependencyProcessingContext ctx) throws RepositoryException {
        ctx.setDependency(node);
        try {
            appDepProcessor.process(ctx);
            if (ctx.isReprocess() || ctx.isUpdated()) {
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
        for (DependencyNode child : children) {
                injected |= injectDeps(child, ctx);
        }
        return injected;
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
        final Dependency dep = node.getDependency();
        if(dep != null) {
            buf.append('(').append(dep.getScope()).append(')');
        }
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
