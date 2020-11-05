package io.quarkus.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.util.IoUtils;

@Mojo(name = "platform-properties", threadSafe = true)
public class PlatformPropertiesMojo extends AbstractMojo {

    @Component
    private MavenProjectHelper projectHelper;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Skip the execution of this mojo
     */
    @Parameter(defaultValue = "true", property = "platformPropertiesInBom")
    private boolean platformPropertiesInBom = true;

    @Parameter(defaultValue = "platform-properties.properties", property = "platformPropertiesFileName")
    private String propertiesFileName = "platform-properties.properties";

    @Parameter(property = "skipPlatformPrefixCheck")
    boolean skipPlatformPrefixCheck;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (!project.getArtifactId().endsWith(BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX)) {
            throw new MojoExecutionException("Project's artifactId " + project.getArtifactId() + " does not end with "
                    + BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX);
        }

        final File propsFile = new File(project.getBuild().getOutputDirectory(), propertiesFileName);
        if (!propsFile.exists()) {
            throw new MojoExecutionException("Failed to locate " + propsFile);
        }

        if (!skipPlatformPrefixCheck) {
            final Properties props = new Properties();
            try (InputStream is = new FileInputStream(propsFile)) {
                props.load(is);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to read properties " + propsFile, e);
            }
            StringBuilder buf = null;
            for (Object name : props.keySet()) {
                if (!String.valueOf(name).startsWith(BootstrapConstants.PLATFORM_PROPERTY_PREFIX)) {
                    if (buf == null) {
                        buf = new StringBuilder()
                                .append("The following platform properties are missing the '")
                                .append(BootstrapConstants.PLATFORM_PROPERTY_PREFIX)
                                .append("' prefix: ");
                    } else {
                        buf.append(", ");
                    }
                    buf.append(name);
                }
            }
            if (buf != null) {
                throw new MojoExecutionException(buf.toString());
            }
        }

        if (platformPropertiesInBom) {
            assertPlatformPropertiesInBom();
        }

        // this is necessary to sometimes be able to resolve the artifacts from the workspace
        final File published = new File(project.getBuild().getDirectory(),
                project.getArtifactId() + "-" + project.getVersion() + ".properties");
        try {
            IoUtils.copy(propsFile.toPath(), published.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy " + propsFile + " to " + published, e);
        }

        projectHelper.attachArtifact(project, "properties", published);
    }

    private void assertPlatformPropertiesInBom() throws MojoExecutionException {
        boolean platformPropsManaged = false;
        for (Dependency dep : dependencyManagement()) {
            if (dep.getArtifactId().equals(project.getArtifactId())
                    && dep.getGroupId().equals(project.getGroupId())
                    && dep.getVersion().equals(project.getVersion())) {
                platformPropsManaged = true;
                break;
            }
        }
        if (!platformPropsManaged) {
            throw new MojoExecutionException("The project's dependencyManagement does not appear to include "
                    + project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion());
        }
    }

    private List<Dependency> dependencyManagement() {
        return project.getDependencyManagement() == null ? null : project.getDependencyManagement().getDependencies();
    }
}
