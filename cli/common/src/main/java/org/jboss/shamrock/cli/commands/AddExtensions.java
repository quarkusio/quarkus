package org.jboss.shamrock.cli.commands;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jboss.shamrock.dependencies.Extension;
import org.jboss.shamrock.maven.utilities.MojoUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.jboss.shamrock.maven.utilities.MojoUtils.getBomArtifactId;
import static org.jboss.shamrock.maven.utilities.MojoUtils.readPom;

public class AddExtensions {
    private Model model;
    private File pom;

    public AddExtensions(final File pom) throws IOException {
        this.model = MojoUtils.readPom(pom);
        this.pom = pom;
    }

    public static List<Extension> get() {
        ObjectMapper mapper = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);
        URL url = AddExtensions.class.getClassLoader().getResource("extensions.json");
        try {
            return mapper.readValue(url, new TypeReference<List<Extension>>() {
                // Do nothing.
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to load the extensions.json file", e);
        }
    }

    public boolean addExtensions(final List<String> extensions) throws IOException {
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
                    System.out.println("Adding extension " + extension.toCoordinates());
                    model.addDependency(extension
                                            .toDependency(containsBOM(model) &&
                                                          isDefinedInBom(dependenciesFromBom, extension)));
                    updated = true;
                } else {
                    System.out.println("Skipping extension " + extension.toCoordinates() + ": already present");
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
            return readPom(getClass().getResourceAsStream("/shamrock-bom/pom.xml"))
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
