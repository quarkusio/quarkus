package io.quarkus.maven.dependency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.paths.EmptyPathTree;
import io.quarkus.paths.FilteredPathTree;
import io.quarkus.paths.ManifestAttributes;
import io.quarkus.paths.MultiRootPathTree;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathFilter;
import io.quarkus.paths.PathTree;
import io.smallrye.classfile.Attributes;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.attribute.ModuleAttribute;

public interface ResolvedDependency extends Dependency {

    PathCollection getResolvedPaths();

    /**
     * A collection of all the resolved direct dependencies of the artifact of this dependency.
     * <p/>
     * Note that this method will include only direct dependencies that are found among application dependencies.
     * This is one of the differences between this method and {@link #getDirectDependencies()}.
     *
     * @return all the resolved direct dependencies of the artifact of this dependency
     */
    Collection<ArtifactCoords> getDependencies();

    /**
     * A collection of the all the configured direct dependencies of the artifact of this dependency
     * (except test dependencies of transitive dependencies).
     * <p/>
     * Note that some of the configured direct dependencies might not be resolved due to scope, optionality
     * or exclusions. The resulting collection returned from this method will still include such dependencies
     * with {@link DependencyFlags#MISSING_FROM_APPLICATION} flag.
     * <p/>
     * Every dependency returned from this method will {@link DependencyFlags#DIRECT} set.
     *
     * @return all the direct dependencies of the artifact of this dependency
     */
    Collection<Dependency> getDirectDependencies();

    /**
     * {@return the module name for this resolved dependency's artifact (not {@code null})}
     */
    String getModuleName();

    default boolean isResolved() {
        final PathCollection paths = getResolvedPaths();
        return paths != null && !paths.isEmpty();
    }

    default WorkspaceModule getWorkspaceModule() {
        return null;
    }

    default ArtifactSources getSources() {
        final WorkspaceModule module = getWorkspaceModule();
        return module == null ? null : module.getSources(getClassifier());
    }

    default PathTree getContentTree() {
        return getContentTree(null);
    }

    default PathTree getContentTree(PathFilter pathFilter) {
        final WorkspaceModule module = getWorkspaceModule();
        final PathTree workspaceTree = module == null ? EmptyPathTree.getInstance() : module.getContentTree(getClassifier());
        if (!workspaceTree.isEmpty()) {
            return pathFilter == null ? workspaceTree : new FilteredPathTree(workspaceTree, pathFilter);
        }
        final PathCollection paths = getResolvedPaths();
        if (paths == null || paths.isEmpty()) {
            return EmptyPathTree.getInstance();
        }
        if (paths.isSinglePath()) {
            final Path p = paths.getSinglePath();
            return isJar() ? PathTree.ofDirectoryOrArchive(p, pathFilter) : PathTree.ofDirectoryOrFile(p, pathFilter);
        }
        final PathTree[] trees = new PathTree[paths.size()];
        int i = 0;
        for (Path p : paths) {
            trees[i++] = PathTree.ofDirectoryOrArchive(p, pathFilter);
        }
        return new MultiRootPathTree(trees);
    }

    static String computeModuleName(ResolvedDependency dep) {
        // first, see if there is a descriptor
        PathTree contentTree = dep.getContentTree();
        if (contentTree.contains("module-info.class")) {
            ClassModel cm = contentTree.apply("module-info.class", pv -> {
                try {
                    return ClassFile.of().parse(Files.readAllBytes(pv.getPath()));
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to read or parse module-info.class", e);
                }
            });
            Optional<ModuleAttribute> optModAttr = cm.findAttribute(Attributes.module());
            if (optModAttr.isPresent()) {
                return optModAttr.get().moduleName().name().stringValue();
            }
        }
        // next, see if there is an explicit automatic module name in the manifest
        ManifestAttributes ma = contentTree.getManifestAttributes();
        if (ma != null) {
            String moduleName = ma.automaticModuleName();
            if (moduleName != null) {
                return moduleName;
            }
        }
        // next, see if we have a (temporary) policy-defined module name for this artifact
        String groupId = dep.getGroupId();
        String artifactId = dep.getArtifactId();
        String classifier = dep.getClassifier();
        // XXX MANUAL AUTOMATIC MODULE NAME FIXUPS
        String moduleName = switch (groupId) {
            case "io.quarkus" -> switch (artifactId) {
                case "quarkus-arc" -> "io.quarkus.arc.runtime";
                default -> null;
            };
            case "io.smallrye.config" -> switch (artifactId) {
                case "smallrye-config-core" -> "io.smallrye.config";
                case "smallrye-config" -> "io.smallrye.config.inject";
                default -> null;
            };
            case "io.smallrye.certs" -> switch (artifactId) {
                // https://github.com/smallrye/smallrye-certificate-generator/issues/41
                case "smallrye-private-key-pem-parser" -> "io.smallrye.certs.pem.private_keys";
                default -> null;
            };
            case "org.jboss.logging" -> switch (artifactId) {
                // https://github.com/jboss-logging/commons-logging-jboss-logging/issues/20
                case "commons-logging-jboss-logging" -> "org.apache.commons.logging";
                default -> null;
            };
            case "org.jboss.slf4j" -> switch (artifactId) {
                // https://github.com/jboss-logging/slf4j-jboss-logmanager/issues/69
                case "slf4j-jboss-logmanager" -> "org.jboss.logmanager.slf4j";
                default -> null;
            };
            case "org.eclipse.microprofile.config" -> switch (artifactId) {
                // https://github.com/microprofile/microprofile-config/issues/768
                case "microprofile-config-api" -> "org.eclipse.microprofile.config";
                default -> null;
            };
            case "org.eclipse.microprofile.rest.client" -> switch (artifactId) {
                // https://github.com/microprofile/microprofile-rest-client/issues/397
                case "microprofile-rest-client-api" -> "org.eclipse.microprofile.rest.client";
                default -> null;
            };
            case "org.eclipse.microprofile.context-propagation" -> switch (artifactId) {
                // https://github.com/microprofile/microprofile-context-propagation/issues/221
                case "microprofile-context-propagation-api" -> "org.eclipse.microprofile.context";
                default -> null;
            };
            default -> null;
        };
        if (moduleName == null) {
            // Generate an automatic module name using a the groupId:artifactId (instead of just the artifactId)
            List<String> groupParts = List.of(groupId.split("[-_.]"));
            List<String> artifactParts = List.of(artifactId.split("[-_.]"));
            StringBuilder finalNameBuilder = new StringBuilder(
                    groupId.length() + artifactId.length() + 1);
            int groupSize = groupParts.size();
            for (int idx = 0; idx < groupSize; idx++) {
                // fast check
                if (groupParts.get(idx).equals(artifactParts.get(0))) {
                    // slower check
                    int overlap = groupSize - idx;
                    if (groupParts.subList(idx, groupSize).equals(artifactParts.subList(0, overlap))) {
                        // cut out the overlapping parts from the artifact list
                        artifactParts = artifactParts.subList(overlap, artifactParts.size());
                        break;
                    }
                }
            }
            finalNameBuilder.append(groupParts.get(0));
            for (int i = 1; i < groupSize; i++) {
                finalNameBuilder.append('.').append(groupParts.get(i));
            }
            for (String artifactPart : artifactParts) {
                finalNameBuilder.append('.').append(artifactPart);
            }
            if (classifier != null && !classifier.isEmpty()) {
                finalNameBuilder.append('.').append(classifier);
            }
            moduleName = finalNameBuilder.toString();
        }
        return moduleName;
    }
}
