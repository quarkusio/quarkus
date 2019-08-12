package io.quarkus.cli.commands;

import static io.quarkus.maven.utilities.MojoUtils.readPom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.GradleBuildFile;
import io.quarkus.cli.commands.file.MavenBuildFile;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.generators.BuildTool;
import io.quarkus.maven.utilities.MojoUtils;

public class AddExtensions {

    private BuildFile buildFile;
    private final static Printer PRINTER = new Printer();

    public AddExtensions(final ProjectWriter writer) throws IOException {
        this(writer, BuildTool.MAVEN);
    }

    public AddExtensions(final ProjectWriter writer, final BuildTool buildTool) throws IOException {
        switch (buildTool) {
            case GRADLE:
                this.buildFile = new GradleBuildFile(writer);
                break;
            case MAVEN:
            default:
                this.buildFile = new MavenBuildFile(writer);
        }
    }

    /**
     * Selection algorithm.
     *
     * @param query the query
     * @param extensions the extension list
     * @param labelLookup whether or not the query must be tested against the labels of the extensions. Should
     *        be {@code false} by default.
     * @return the list of matching candidates and whether or not a match has been found.
     */
    static SelectionResult select(String query, List<Extension> extensions, boolean labelLookup) {
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
        List<Extension> matchesLabels;
        if (labelLookup) {
            matchesLabels = extensions.stream()
                    .filter(extension -> extension.labels().contains(q)).collect(Collectors.toList());
        } else {
            matchesLabels = new ArrayList<>();
        }

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
        return artifactId.equalsIgnoreCase(q) ||
                artifactId.equalsIgnoreCase("quarkus-" + q);
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
                updated = buildFile.addExtensionAsGAV(query) || updated;
            } else {
                SelectionResult result = select(query, registry, false);
                if (!result.matches()) {
                    StringBuilder sb = new StringBuilder();
                    // We have 3 cases, we can still have a single candidate, but the match is on label
                    // or we have several candidates, or none
                    Set<Extension> candidates = result.getExtensions();
                    if (candidates.isEmpty()) {
                        // No matches at all.
                        PRINTER.nok(" Cannot find a dependency matching '" + query + "', maybe a typo?");
                        success = false;
                    } else {
                        sb.append(Printer.NOK).append(" Multiple extensions matching '").append(query).append("'");
                        result.getExtensions()
                                .forEach(extension -> sb.append(System.lineSeparator()).append("     * ")
                                        .append(extension.managementKey()));
                        sb.append(System.lineSeparator())
                                .append("     Be more specific e.g using the exact name or the full GAV.");
                        PRINTER.print(sb.toString());
                        success = false;
                    }
                } else { // Matches.
                    final Extension extension = result.getMatch();
                    // Don't set success to false even if the dependency is not added; as it's should be idempotent.
                    updated = buildFile.addDependency(dependenciesFromBom, extension) || updated;
                }
            }
        }

        if (updated) {
            buildFile.write();
        }

        return new AddExtensionResult(updated, success);
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

}
