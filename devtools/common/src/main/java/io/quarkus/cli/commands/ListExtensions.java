package io.quarkus.cli.commands;

import static io.quarkus.maven.utilities.MojoUtils.credentials;
import static io.quarkus.maven.utilities.MojoUtils.getPluginVersion;
import static io.quarkus.maven.utilities.MojoUtils.loadExtensions;
import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.maven.utilities.QuarkusDependencyPredicate;

public class ListExtensions {
    private static final String FORMAT = "%-8s %-20s %-50s %s";
    private Model model;

    public ListExtensions(final Model model) {
        this.model = model;
    }

    public void listExtensions() {
        System.out.println("\nCurrent Quarkus extensions available: ");
        System.out.println(String.format(FORMAT, "Status", "Extension", "ArtifactId", "Updated Version"));

        final Map<String, Dependency> installed = findInstalled();

        loadExtensions().forEach(extension -> display(extension, installed));
    }

    private void display(Extension extension, final Map<String, Dependency> installed) {
        final Dependency dependency = installed.get(String.format("%s:%s", extension.getGroupId(), extension.getArtifactId()));

        String label = "";
        String version = "";

        final String extracted = extractVersion(dependency);
        if (extracted != null) {
            if (MojoUtils.getPluginVersion().equalsIgnoreCase(extracted)) {
                label = "current";
            } else {
                label = "update";
                version = String.format("%s <> %s", extracted, getPluginVersion());
            }
        }

        System.out.println(String.format(FORMAT, label, extension.getName(), extension.getArtifactId(), version));
    }

    private String extractVersion(final Dependency dependency) {
        String version = dependency != null ? dependency.getVersion() : null;
        if (version != null && version.startsWith("$")) {
            final String value = (String) model.getProperties().get(propertyName(version));
            if (value != null) {
                version = value;
            }
        }
        return version;
    }

    private String propertyName(final String variable) {
        return variable.substring(2, variable.length() - 1);
    }

    Map<String, Dependency> findInstalled() {
        return mapDependencies(model.getDependencies(), loadManaged());
    }

    private Map<String, Dependency> loadManaged() {
        final DependencyManagement managed = model.getDependencyManagement();
        return managed != null ? mapDependencies(managed.getDependencies(), Collections.emptyMap())
                : Collections.emptyMap();
    }

    private Map<String, Dependency> mapDependencies(final List<Dependency> dependencies,
            final Map<String, Dependency> managed) {
        final Map<String, Dependency> map = new TreeMap<>();

        if (dependencies != null) {
            final List<Dependency> listed = dependencies.stream()
                    .filter(new QuarkusDependencyPredicate())
                    .collect(toList());

            listed.forEach(d -> {
                if (d.getVersion() == null) {
                    final Dependency managedDep = managed.get(credentials(d));
                    if (managedDep != null) {
                        final String version = managedDep.getVersion();
                        if (version != null) {
                            d.setVersion(version);
                        }
                    }
                }

                map.put(credentials(d), d);
            });
        }
        return map;
    }

}
