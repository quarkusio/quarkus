package org.jboss.shamrock.forge;

import com.google.common.base.Strings;
import org.apache.maven.model.Model;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.maven.plugins.ConfigurationElement;
import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ForgeUtils {

  public static void addPropertyToProject(Project project, String key, String value) {
    MavenFacet maven = project.getFacet(MavenFacet.class);
    Model pom = maven.getModel();
    Properties properties = pom.getProperties();
    properties.setProperty(key, value);
    maven.setModel(pom);
  }

  public static Dependency getOrAddDependency(Project project, String groupId, String artifactId,
                                              String version, String classifier, String scope) {
    DependencyFacet dependencies = project.getFacet(DependencyFacet.class);
    Optional<Dependency> found = dependencies.getEffectiveDependencies().stream().filter(dep ->
        dep.getCoordinate().getGroupId().equalsIgnoreCase(groupId)
            && dep.getCoordinate().getArtifactId().equalsIgnoreCase(artifactId)
            && (version == null || version.equalsIgnoreCase(dep.getCoordinate().getVersion()))
            && Strings.isNullOrEmpty(dep.getCoordinate().getClassifier())
            && (Strings.isNullOrEmpty(dep.getCoordinate().getPackaging()) || dep.getCoordinate().getPackaging()
            .equalsIgnoreCase("jar"))
    ).findAny();
    if (found.isPresent()) {
      return found.get();
    }

    DependencyBuilder dependency = DependencyBuilder.create().setGroupId(groupId).setArtifactId(artifactId);
    if (version != null) {
      dependency.setVersion(version);
    }
    if (scope != null) {
      dependency.setScopeType(scope);
    }
    if (classifier != null  && ! classifier.isEmpty()) {
      dependency.setClassifier(classifier);
    }

    dependencies.addDirectDependency(dependency);

    return dependency;
  }
}
