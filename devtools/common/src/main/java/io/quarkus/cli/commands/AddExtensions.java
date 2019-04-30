package io.quarkus.cli.commands;

import static io.quarkus.maven.utilities.MojoUtils.getBomArtifactId;
import static io.quarkus.maven.utilities.MojoUtils.readPom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;

public class AddExtensions {
    private static final String EXTENSION_ARTIFACTID_PREFIX = "quarkus-";
    private static final String QUARKUS_GROUPID = "io.quarkus";
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

        List<Extension> loadedExtensions = MojoUtils.loadExtensions();
        for (String dependency : extensions) {
            dependency = dependency.trim();
            Dependency res;
            if (!dependency.contains(":")) {
                final String groupId = QUARKUS_GROUPID;
                String artifactId;
                if (!dependency.startsWith(EXTENSION_ARTIFACTID_PREFIX)) {
                    artifactId = EXTENSION_ARTIFACTID_PREFIX + dependency.toLowerCase();
                } else {
                    artifactId = dependency.toLowerCase();
                }
                res = new Dependency();
                res.setGroupId(groupId);
                res.setArtifactId(artifactId);
            } else {
                if (dependency.startsWith(QUARKUS_GROUPID)) {
                    dependency = dependency.toLowerCase();
                }
                res = MojoUtils.parse(dependency);
            }

            if (QUARKUS_GROUPID.equals(res.getGroupId())) {
                Optional<Extension> optional = loadedExtensions.stream()
                        .filter(d -> {
                            return d.getArtifactId()
                                    .equals(res.getArtifactId());
                        })
                        .findAny();

                if (!optional.isPresent()) {
                    throw new IllegalArgumentException("Cannot find a Quarkus extension matching " + dependency);
                } else if ((res.getVersion() == null || res.getVersion().isEmpty())) {
                    res.setVersion(optional.get().getVersion());
                }
            }

            if (!MojoUtils.hasDependency(model, res.getGroupId(), res.getArtifactId())) {
                if (res.getVersion() != null && containsBOM(model) &&
                        isDefinedInBom(dependenciesFromBom, res)) {
                    res.setVersion(null);
                }
                System.out.println(
                        String.format("Adding extension %s:%s%s", res.getGroupId(), res.getArtifactId(),
                                res.getVersion() != null ? ":" + res.getVersion() : ""));
                model.addDependency(res);
                updated = true;
            } else {
                System.out.println(
                        String.format("Skipping extension %s:%s : already present", res.getGroupId(), res.getArtifactId()));
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
                .anyMatch(dependency -> dependency.getGroupId().equalsIgnoreCase(QUARKUS_GROUPID)
                        && dependency.getArtifactId().equalsIgnoreCase(getBomArtifactId()));
    }

    private boolean isDefinedInBom(List<Dependency> dependencies, Dependency dep) {
        return dependencies.stream().anyMatch(dependency -> dependency.getGroupId().equalsIgnoreCase(dep.getGroupId())
                && dependency.getArtifactId().equalsIgnoreCase(dep.getArtifactId()));
    }
}
