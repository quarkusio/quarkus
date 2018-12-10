package org.jboss.shamrock.forge;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.projects.MavenBuildSystem;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@RunWith(Arquillian.class)
public class ShamrockFacetTest {


    @Deployment
    @AddonDependencies
    public static AddonArchive getDeployment() {
        return ShrinkWrap
                       .create(AddonArchive.class)
                       .addPackages(true, "org.assertj.core")
                       .addBeansXML();
    }

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private MavenBuildSystem projectProvider;

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private ShellTest shellTest;


    @Test
    public void testNewShamrockProject() throws Exception {
        Project project = projectFactory.createTempProject(projectProvider);
        facetFactory.install(project, ShamrockFacet.class);

        DependencyFacet dependencies = project.getFacet(DependencyFacet.class);
        MavenPluginFacet plugins = project.getFacet(MavenPluginFacet.class);
        MavenFacet maven = project.getFacet(MavenFacet.class);

        assertNotNull(maven.getProperties().get(ShamrockFacet.SHAMROCK_VERSION_PROPERTY_NAME));

        // Check dependencies
        checkDependency(dependencies, "org.jboss.shamrock", "shamrock-jaxrs-deployment",
                ShamrockFacet.SHAMROCK_VERSION, "provided");
        checkDependency(dependencies, "org.jboss.shamrock", "shamrock-arc-deployment",
                ShamrockFacet.SHAMROCK_VERSION, "provided");

        assertThat(dependencies.getDependencies()).hasSize(5);

        // Check maven compiler
        hasPlugin(plugins, "maven-compiler-plugin");
        hasPlugin(plugins, "shamrock-maven-plugin");

        // check profile
        assertThat(maven.getModel().getProfiles().size()).isEqualTo(1);

        assertTrue(project.getRoot().getChild("src/main/java/org/acme/quickstart/MyApplication.java").exists());
    }

    private void hasPlugin(MavenPluginFacet plugins, String artifactId) {
        Optional<MavenPlugin> p = plugins.listConfiguredEffectivePlugins().stream().filter((plugin) -> plugin
            .getCoordinate().getArtifactId().equals(artifactId)).findFirst();
        assertThat(p.get()).isNotNull();
    }

    public void checkDependency(DependencyFacet dependencies, String groupId, String artifactId, String version, String
        scope) {
        Optional<Dependency> dep = dependencies.getDependencies().stream().filter((dependency) ->
            dependency.getCoordinate().getArtifactId().equals(artifactId))
            .findFirst();
        assertThat(dep.isPresent()).isTrue();
        assertThat(dep.get().getCoordinate().getGroupId()).isEqualTo(groupId);
        if (version == null) {
            assertThat(dep.get().getCoordinate().getVersion()).isNull();
        } else {
            assertThat(dep.get().getCoordinate().getVersion()).isEqualTo(version);
        }
        if (scope != null) {
            assertThat(dep.get().getScopeType()).isEqualTo(scope);
        }
    }
}
