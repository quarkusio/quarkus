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

package io.quarkus.bootstrap.resolver.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;

/**
 *
 * @author Alexey Loubyansky
 */
public class DependencyGraphParser {

    private DependencyNode root;
    private List<DependencyNode> stack = new ArrayList<>();
    private int depth;

    public DependencyNode parse(Path p) throws BootstrapDependencyProcessingException {
        root = null;
        stack.clear();
        depth = 0;
        try(BufferedReader reader = Files.newBufferedReader(p)) {
            readGraph(reader);
            return root;
        } catch (IOException e) {
            throw new BootstrapDependencyProcessingException("Failed to parse dependency graph " + p, e);
        }
    }

    private void readGraph(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        while(line != null) {
            readNode(line);
            line = reader.readLine();
        }
    }

    private void readNode(String line) throws IOException {
        int i = 0;
        while(i < line.length()) {
            if(line.charAt(i) != ' ') {
                break;
            }
            ++i;
        }
        if(i > depth) {
            throw new IllegalStateException("Unexpected offset for `" + line + "`");
        }
        final DefaultDependencyNode node = new DefaultDependencyNode(toDependency(line, i));
        try {
            node.setVersionConstraint(new GenericVersionScheme().parseVersionConstraint(node.getArtifact().getVersion()));
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalStateException(e);
        }
        if(depth == 0) {
            root = node;
        } else {
            while (i < depth) {
                stack.remove(--depth);
            }
            stack.get(depth - 1).getChildren().add(node);
        }
        stack.add(node);
        ++depth;
    }

    public static Artifact toArtifact(String str) {
        return toArtifact(str, 0);
    }

    private static Artifact toArtifact(String str, int offset) {
        String groupId = null;
        String artifactId = null;
        String classifier = "";
        String type = "jar";
        String version = null;

        int colon = str.indexOf(':', offset);
        final int length = str.length();
        if(colon < offset + 1 || colon == length - 1) {
            illegalDependencyFormat(str);
        }
        groupId = str.substring(offset, colon);
        offset = colon + 1;
        colon = str.indexOf(':', offset);
        if(colon < 0) {
            artifactId = str.substring(offset, length);
        } else {
            if(colon == length - 1) {
                illegalDependencyFormat(str);
            }
            artifactId = str.substring(offset, colon);
            offset = colon + 1;
            colon = str.indexOf(':', offset);
            if(colon < 0) {
                version = str.substring(offset, length);
            } else {
                if(colon == length - 1) {
                    illegalDependencyFormat(str);
                }
                type = str.substring(offset, colon);
                offset = colon + 1;
                colon = str.indexOf(':', offset);
                if(colon < 0) {
                    version = str.substring(offset, length);
                } else {
                    if (colon == length - 1) {
                        illegalDependencyFormat(str);
                    }
                    classifier = type;
                    type = str.substring(offset, colon);
                    version = str.substring(colon + 1);
                }
            }
        }
        return new DefaultArtifact(groupId, artifactId, classifier, type, version);
    }

    private static Dependency toDependency(String str, int offset) {
        String groupId = null;
        String artifactId = null;
        String classifier = "";
        String type = "jar";
        String version = null;

        int sep = str.indexOf(':', offset);
        final int length = str.length();
        if(sep < offset + 1 || sep == length - 1) {
            illegalDependencyFormat(str);
        }
        groupId = str.substring(offset, sep);
        offset = sep + 1;
        sep = str.indexOf(':', offset);
        if(sep < 0) {
            artifactId = str.substring(offset, length);
        } else {
            if(sep == length - 1) {
                illegalDependencyFormat(str);
            }
            artifactId = str.substring(offset, sep);
            offset = sep + 1;
            sep = str.indexOf(':', offset);
            if(sep < 0) {
                version = str.substring(offset, length);
            } else {
                if(sep == length - 1) {
                    illegalDependencyFormat(str);
                }
                type = str.substring(offset, sep);
                offset = sep + 1;
                sep = str.indexOf(':', offset);
                if(sep < 0) {
                    version = str.substring(offset, length);
                } else {
                    if (sep == length - 1) {
                        illegalDependencyFormat(str);
                    }
                    classifier = type;
                    type = str.substring(offset, sep);
                    version = str.substring(sep + 1);
                }
            }
        }

        String scope = null;
        sep = version.lastIndexOf('(');
        if(sep > 0) {
            scope = version.substring(sep + 1, version.length() - 1);
            version = version.substring(0, sep).trim();
        }

        return new Dependency(new DefaultArtifact(groupId, artifactId, classifier, type, version), scope);
    }

    private static void illegalDependencyFormat(String str) {
        throw new IllegalArgumentException("Bad artifact coordinates " + str
                + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
    }
}
