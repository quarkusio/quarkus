package io.quarkus.deployment.dev;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevModeCleanupBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Cleans up classes that is not needed in Prod
 */
public class DevModeCleanupProcessor {

    /**
     * This makes sure the Dev Mode Classes are not included in the prod build
     */
    @BuildStep(onlyIf = IsNormal.class)
    void cleanProd(BuildProducer<RemovedResourceBuildItem> producer,
            List<DevModeCleanupBuildItem> devUIItemsToRemove,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {

        Set<String> classNames = new HashSet<>();
        Set<String> packageNames = new HashSet<>();
        for (DevModeCleanupBuildItem devUIRemoveItemBuildItem : devUIItemsToRemove) {
            if (devUIRemoveItemBuildItem.isWholePackage()) {
                packageNames.addAll(devUIRemoveItemBuildItem.getPackageNames());
            } else {
                for (Class c : devUIRemoveItemBuildItem.getClasses()) {
                    classNames.add(c.getName());
                }
            }
        }

        List<RemovedResourceBuildItem> removedResourceBuildItems = getRemoveResourceBuildItemList(classNames, packageNames,
                curateOutcomeBuildItem);
        producer.produce(removedResourceBuildItems);
    }

    private List<RemovedResourceBuildItem> getRemoveResourceBuildItemList(Set<String> classNames, Set<String> packageNames,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        List<RemovedResourceBuildItem> buildItems = new ArrayList<>();

        ApplicationModel applicationModel = curateOutcomeBuildItem.getApplicationModel();
        Collection<ResolvedDependency> runtimeDependencies = applicationModel.getRuntimeDependencies();
        List<ResolvedDependency> allArtifacts = new ArrayList<>(runtimeDependencies.size() + 1);
        allArtifacts.addAll(runtimeDependencies);
        allArtifacts.add(applicationModel.getAppArtifact());
        for (ResolvedDependency i : allArtifacts) {
            for (Path path : i.getResolvedPaths()) {
                if (path.toString().endsWith(".jar") && path.toFile().isFile()) {
                    Set<String> foundClasses = new HashSet<>();
                    try (JarFile jarFile = new JarFile(path.toFile())) {
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            if (entry.getName().endsWith(".class")) {
                                String entryClassName = entry.getName()
                                        .replace('/', '.')
                                        .replace(".class", "");
                                if (classNames.contains(entryClassName)) {
                                    foundClasses.add(entry.getName());
                                } else if (packageNames.stream().anyMatch(entryClassName::startsWith)) {
                                    foundClasses.add(entry.getName());
                                }
                            }
                        }
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                    if (!foundClasses.isEmpty()) {
                        buildItems.add(new RemovedResourceBuildItem(path, foundClasses));
                    }
                }
            }
        }

        return buildItems;
    }

}
