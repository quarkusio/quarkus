package io.quarkus.cli.commands;

import static io.quarkus.maven.utilities.MojoUtils.credentials;
import static io.quarkus.maven.utilities.MojoUtils.getPluginVersion;
import static io.quarkus.maven.utilities.MojoUtils.loadExtensions;
import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.QuarkusDependencyPredicate;

public class ListExtensions {
    private static final String FULL_FORMAT = "%-8s %-50s %-50s %-25s%n%s";
    private static final String SIMPLE_FORMAT = "%-50s %-50s%n%s";
    private Model model;

    public ListExtensions(final Model model) {
        this.model = model;
    }

    public void listExtensions(boolean all, String format, String search) {
        final Map<String, Dependency> installed = findInstalled();

        Stream<Extension> extensionsStream = loadExtensions().stream();
        if (search != null && !"*".equalsIgnoreCase(search)) {
            final Pattern searchPattern = Pattern.compile(".*" + search + ".*", Pattern.CASE_INSENSITIVE);
            extensionsStream = extensionsStream.filter(e -> filterBySearch(searchPattern, e));
        }
        List<Extension> loadedExtensions = extensionsStream.collect(Collectors.toList());

        if (loadedExtensions.isEmpty()) {
            System.out.println("No extension found with this pattern");
        } else {
            Consumer<String[]> currentFormatter = "simple".equalsIgnoreCase(format) ? this::simpleFormatter
                    : this::fullFormatter;
            String extensionStatus = all ? "available" : "installable";
            System.out.println(String.format("%nCurrent Quarkus extensions %s: ", extensionStatus));
            if (!"simple".equalsIgnoreCase(format)) {
                currentFormatter.accept(new String[] { "Status", "Extension", "ArtifactId", "Updated Version", "Guide" });
            }

            loadedExtensions.forEach(extension -> display(extension, installed, all, currentFormatter));
            System.out.println("\nAdd an extension to your project by adding the dependency to your " +
                    "project or use `mvn quarkus:add-extension -Dextensions=\"artifactId\"`");
        }
    }

    private boolean filterBySearch(final Pattern searchPattern, Extension e) {
        return searchPattern.matcher(e.getName()).matches();
    }

    private void simpleFormatter(String[] cols) {
        System.out.println(String.format(SIMPLE_FORMAT, cols[1], cols[2], cols[4]));
    }

    private void fullFormatter(String[] cols) {
        System.out.println(String.format(FULL_FORMAT, cols[0], cols[1], cols[2], cols[3], cols[4]));
    }

    private void display(Extension extension, final Map<String, Dependency> installed, boolean all,
            Consumer<String[]> formatter) {

        if (!all && installed.containsKey(String.format("%s:%s", extension.getGroupId(), extension.getArtifactId()))) {
            return;
        }
        final Dependency dependency = installed.get(String.format("%s:%s", extension.getGroupId(), extension.getArtifactId()));

        String label = "";
        String version = "";

        final String extracted = extractVersion(dependency);
        if (extracted != null) {
            if (getPluginVersion().equalsIgnoreCase(extracted)) {
                label = "current";
                version = String.format("%s", extracted);
            } else {
                label = "update";
                version = String.format("%s <> %s", extracted, getPluginVersion());
            }
        }

        String guide = StringUtils.defaultString(extension.getGuide(), "");
        formatter.accept(new String[] { label, extension.getName(), extension.getArtifactId(), version, guide });
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
