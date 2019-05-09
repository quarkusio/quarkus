package io.quarkus.cli.commands;

import static io.quarkus.maven.utilities.MojoUtils.getBomArtifactId;
import static io.quarkus.maven.utilities.MojoUtils.readPom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;

public class AddExtensions {
    private static String OK = "\u2705";
    private static String NOK = "\u274c";
    private static String NOOP = "\uD83D\uDC4D";
    private Model model;
    private String pom;
    private ProjectWriter writer;

    public AddExtensions(final ProjectWriter writer, final String pom) throws IOException {
        this.model = MojoUtils.readPom(new ByteArrayInputStream(writer.getContent(pom)));
        this.writer = writer;
        this.pom = pom;
    }

    public boolean addExtensions(final Set<String> extensions) throws IOException {
        if (extensions == null || extensions.isEmpty()) {
            return false;
        }

        boolean updated = false;
        List<Dependency> dependenciesFromBom = getDependenciesFromBom();

        for (String dependency : extensions) {
            List<Extension> matches = MojoUtils.loadExtensions().stream()
                    .filter(d -> {
                        boolean hasTag = d.labels().contains(dependency.trim().toLowerCase());
                        boolean machName = d.getName()
                                .toLowerCase()
                                .contains(dependency.trim().toLowerCase());
                        boolean matchArtifactId = d.getArtifactId()
                                .toLowerCase()
                                .contains(dependency.trim().toLowerCase());
                        return hasTag || machName || matchArtifactId;
                    })
                    .collect(Collectors.toList());

            if (matches.size() > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append(NOK)
                        .append(" Multiple extensions matching '" + dependency + "'");

                matches.stream()
                        .forEach(extension -> sb.append(System.lineSeparator()).append("     * ")
                                .append(extension.managementKey()));
                sb.append(System.lineSeparator()).append("     Be more specific e.g using the exact name or the full gav.");
                System.out.println(sb);
            } else if (matches.size() == 1) {
                final Extension extension = matches.get(0);

                if (!MojoUtils.hasDependency(model, extension.getGroupId(), extension.getArtifactId())) {
                    System.out.println(OK + " Adding extension " + extension.managementKey());
                    model.addDependency(extension
                            .toDependency(containsBOM(model) &&
                                    isDefinedInBom(dependenciesFromBom, extension)));
                    updated = true;
                } else {
                    System.out.println(NOOP + " Skipping extension " + extension.managementKey() + ": already present");
                }
            } else if (dependency.contains(":")) {
                Dependency parsed = MojoUtils.parse(dependency);
                System.out.println(OK + " Adding dependency " + parsed.getManagementKey());
                model.addDependency(parsed);
                updated = true;
            } else {
                System.out.println(NOK + " Cannot find a dependency matching '" + dependency + "', maybe a typo?");
            }
        }

        if (updated) {
            ByteArrayOutputStream pomOutputStream = new ByteArrayOutputStream();
            MojoUtils.write(model, pomOutputStream);
            writer.write(pom, pomOutputStream.toString());
        }

        return updated;
    }

    private List<Dependency> getDependenciesFromBom() {
        try {
            return readPom(getClass().getResourceAsStream("/quarkus-bom/pom.xml"))
                    .getDependencyManagement()
                    .getDependencies();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
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
