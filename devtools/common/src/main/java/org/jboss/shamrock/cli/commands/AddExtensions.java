package io.quarkus.cli.commands;

import static io.quarkus.maven.utilities.MojoUtils.getBomArtifactId;
import static io.quarkus.maven.utilities.MojoUtils.readPom;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;

public class AddExtensions {
    private Model model;
    private File pom;

    public AddExtensions(final File pom) throws IOException {
        this.model = MojoUtils.readPom(pom);
        this.pom = pom;
    }

    public boolean addExtensions(final Set<String> extensions) throws IOException {
        if (extensions == null || extensions.isEmpty()) {
            return false;
        }

        boolean updated = false;
        List<Dependency> dependenciesFromBom = getDependenciesFromBom();

        for (String dependency : extensions) {
            Optional<Extension> optional = MojoUtils.loadExtensions().stream()
                                                    .filter(d -> {
                                                               boolean hasTag = d.labels().contains(dependency.trim().toLowerCase());
                                                               boolean machName = d.getName()
                                                                                   .toLowerCase()
                                                                                   .contains(dependency.trim().toLowerCase());
                                                               return hasTag || machName;
                                                           })
                                                    .findAny();

            if (optional.isPresent()) {
                final Extension extension = optional.get();

                if (!MojoUtils.hasDependency(model, extension.getGroupId(), extension.getArtifactId())) {
                    System.out.println("Adding extension " + extension.managementKey());
                    model.addDependency(extension
                                            .toDependency(containsBOM(model) &&
                                                          isDefinedInBom(dependenciesFromBom, extension)));
                    updated = true;
                } else {
                    System.out.println("Skipping extension " + extension.managementKey() + ": already present");
                }
            } else if (dependency.contains(":")) {
                Dependency parsed = MojoUtils.parse(dependency);
                System.out.println("Adding dependency " + parsed.getManagementKey());
                model.addDependency(parsed);
                updated = true;
            } else {
                System.out.println("Cannot find a dependency matching '" + dependency + "'");
            }
        }

        if (updated) {
            MojoUtils.write(model, pom);
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
        return dependencies.stream().anyMatch(dependency ->
                                                  dependency.getGroupId().equalsIgnoreCase(extension.getGroupId())
                                                  && dependency.getArtifactId().equalsIgnoreCase(extension.getArtifactId()));
    }
}
