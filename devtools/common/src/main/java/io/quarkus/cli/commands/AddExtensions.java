package io.quarkus.cli.commands;

import static io.quarkus.maven.utilities.MojoUtils.getBomArtifactId;
import static io.quarkus.maven.utilities.MojoUtils.readPom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;

public class AddExtensions {
    private static final String OK = "\u2705";
    private static final String NOK = "\u274c";
    private static final String NOOP = "\uD83D\uDC4D";

    private Model model;
    private String pom;
    private ProjectWriter writer;

    public AddExtensions(final ProjectWriter writer, final String pom) throws IOException {
        this.model = MojoUtils.readPom(new ByteArrayInputStream(writer.getContent(pom)));
        this.writer = writer;
        this.pom = pom;
    }

    /**
     * Selection algorithm.
     *
     * @param query the query
     * @param extensions the extension list
     * @return the list of matching candidates and whether or not a match has been found.
     */
    static SelectionResult select(String query, List<Extension> extensions) {
        String q = query.trim().toLowerCase();

        // Try exact matches
        Set<Extension> matchesNameOrArtifactId = extensions.stream()
                .filter(extension -> extension.getName().equalsIgnoreCase(q) || matchesArtifactId(extension.getArtifactId(), q))
                .collect(Collectors.toSet());
        if (matchesNameOrArtifactId.size() == 1) {
            return new SelectionResult(matchesNameOrArtifactId, true);
        }

        // Try short names
        Set<Extension> matchesShortName = extensions.stream().filter(extension -> matchesShortName(extension, q))
                .collect(Collectors.toSet());

        if (matchesShortName.size() == 1 && matchesNameOrArtifactId.isEmpty()) {
            return new SelectionResult(matchesShortName, true);
        }

        // Partial matches on name, artifactId and short names
        Set<Extension> partialMatches = extensions.stream().filter(extension -> extension.getName().toLowerCase().contains(q)
                || extension.getArtifactId().toLowerCase().contains(q)
                || extension.getShortName().toLowerCase().contains(q)).collect(Collectors.toSet());
        // Even if we have a single partial match, if the name, artifactId and short names are ambiguous, so not
        // consider it as a match.
        if (partialMatches.size() == 1 && matchesNameOrArtifactId.isEmpty() && matchesShortName.isEmpty()) {
            return new SelectionResult(partialMatches, true);
        }

        // find by labels
        List<Extension> matchesLabels = extensions.stream()
                .filter(extension -> extension.labels().contains(q)).collect(Collectors.toList());

        Set<Extension> candidates = new LinkedHashSet<>();
        candidates.addAll(matchesNameOrArtifactId);
        candidates.addAll(matchesShortName);
        candidates.addAll(partialMatches);
        candidates.addAll(matchesLabels);
        return new SelectionResult(candidates, false);
    }

    private static boolean matchesShortName(Extension extension, String q) {
        return q.equalsIgnoreCase(extension.getShortName());
    }

    private static boolean matchesArtifactId(String artifactId, String q) {
        return (artifactId.equalsIgnoreCase(q) ||
                artifactId.equalsIgnoreCase("quarkus-" + q) ||
                artifactId.equalsIgnoreCase("quarkus-smallrye-" + q));
    }

    public AddExtensionResult addExtensions(final Set<String> extensions) throws IOException {
        if (extensions == null || extensions.isEmpty()) {
            return new AddExtensionResult(false, true);
        }

        boolean updated = false;
        boolean success = true;
        List<Dependency> dependenciesFromBom = getDependenciesFromBom();

        List<Extension> registry = MojoUtils.loadExtensions();
        for (String query : extensions) {

            if (query.contains(":")) {
                // GAV case.
                updated = addExtensionAsGAV(query) || updated;
            } else {
                SelectionResult result = select(query, registry);
                if (!result.matches()) {
                    StringBuilder sb = new StringBuilder();
                    // We have 3 cases, we can still have a single candidate, but the match is on label
                    // or we have several candidates, or none
                    Set<Extension> candidates = result.getExtensions();
                    if (candidates.isEmpty()) {
                        // No matches at all.
                        print(NOK + " Cannot find a dependency matching '" + query + "', maybe a typo?");
                        success = false;
                    } else if (candidates.size() == 1) {
                        sb.append(NOK).append(" One extension matching '").append(query).append("'");
                        sb.append(System.lineSeparator()).append("     * ")
                                .append(candidates.iterator().next().managementKey());
                        sb.append(System.lineSeparator())
                                .append("     Use the exact name or the full GAV to add the extension");
                        print(sb.toString());
                        success = false;
                    } else {
                        sb.append(NOK).append(" Multiple extensions matching '").append(query).append("'");
                        result.getExtensions()
                                .forEach(extension -> sb.append(System.lineSeparator()).append("     * ")
                                        .append(extension.managementKey()));
                        sb.append(System.lineSeparator())
                                .append("     Be more specific e.g using the exact name or the full GAV.");
                        print(sb.toString());
                        success = false;
                    }
                } else { // Matches.
                    final Extension extension = result.getMatch();
                    // Don't set success to false even if the dependency is not added; as it's should be idempotent.
                    updated = addDependency(dependenciesFromBom, extension) || updated;
                }
            }
        }

        if (updated) {
            ByteArrayOutputStream pomOutputStream = new ByteArrayOutputStream();
            MojoUtils.write(model, pomOutputStream);
            writer.write(pom, pomOutputStream.toString("UTF-8"));
        }

        return new AddExtensionResult(updated, success);
    }

    private boolean addDependency(List<Dependency> dependenciesFromBom, Extension extension) {
        if (!MojoUtils.hasDependency(model, extension.getGroupId(), extension.getArtifactId())) {
            print(OK + " Adding extension " + extension.managementKey());
            model.addDependency(extension
                    .toDependency(containsBOM(model) &&
                            isDefinedInBom(dependenciesFromBom, extension)));
            return true;
        } else {
            print(NOOP + " Skipping extension " + extension.managementKey() + ": already present");
            return false;
        }
    }

    private boolean addExtensionAsGAV(String query) {
        Dependency parsed = MojoUtils.parse(query.trim().toLowerCase());
        boolean alreadyThere = model.getDependencies().stream()
                .anyMatch(d -> d.getManagementKey().equalsIgnoreCase(parsed.getManagementKey()));
        if (!alreadyThere) {
            print(OK + " Adding dependency " + parsed.getManagementKey());
            model.addDependency(parsed);
            return true;
        } else {
            print(NOOP + " Dependency " + parsed.getManagementKey() + " already in the pom.xml file - skipping");
            return false;
        }
    }

    private void print(String message) {
        System.out.println(message);
    }

    private List<Dependency> getDependenciesFromBom() {
        try {
            return readPom(getClass().getResourceAsStream("/quarkus-bom/pom.xml"))
                    .getDependencyManagement()
                    .getDependencies();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read the BOM file: " + e.getMessage(), e);
        }
    }

    private boolean containsBOM(Model model) {
        if (model.getDependencyManagement() == null) {
            return false;
        }
        List<Dependency> dependencies = model.getDependencyManagement().getDependencies();
        return dependencies.stream()
                // Find bom
                .filter(dependency -> "import".equalsIgnoreCase(dependency.getScope()))
                .filter(dependency -> "pom".equalsIgnoreCase(dependency.getType()))
                // Does it matches the bom artifact name
                .anyMatch(dependency -> dependency.getArtifactId().equalsIgnoreCase(getBomArtifactId()));
    }

    private boolean isDefinedInBom(List<Dependency> dependencies, Extension extension) {
        return dependencies.stream().anyMatch(dependency -> dependency.getGroupId().equalsIgnoreCase(extension.getGroupId())
                && dependency.getArtifactId().equalsIgnoreCase(extension.getArtifactId()));
    }
}
