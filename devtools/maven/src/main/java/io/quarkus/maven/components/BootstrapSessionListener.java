package io.quarkus.maven.components;

import java.io.IOException;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;

import io.quarkus.maven.BuildAnalyticsProvider;
import io.quarkus.maven.QuarkusBootstrapProvider;

@Singleton
@Named("quarkus-bootstrap")
public class BootstrapSessionListener extends AbstractMavenLifecycleParticipant {

    private final Logger logger;
    private final QuarkusBootstrapProvider bootstrapProvider;
    private final BuildAnalyticsProvider buildAnalyticsProvider;

    private boolean enabled;

    @Inject
    public BootstrapSessionListener(Logger logger, QuarkusBootstrapProvider bootstrapProvider,
            BuildAnalyticsProvider buildAnalyticsProvider) {
        this.logger = logger;
        this.bootstrapProvider = bootstrapProvider;
        this.buildAnalyticsProvider = buildAnalyticsProvider;
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        try {
            bootstrapProvider.close();
            buildAnalyticsProvider.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterProjectsRead(MavenSession session) {
        // if this method is called then Maven plugin extensions are enabled
        enabled = true;

        // Now let's automatically apply the required JVM flags to Surefire;
        // bear in mind that this is just a convenience: we can't rely on Maven
        // extensions to be enabled - but when they are, we can make it more useful.
        injectSurefireTuning(session);
    }

    private void injectSurefireTuning(MavenSession session) {
        logger.debug("Quarkus Maven extension: inspecting Surefire configuration");
        for (MavenProject project : session.getProjects()) {
            //Let's not interfere with non-Quarkus modules:
            if (isQuarkusPluginConfigured(project)) {
                injectSurefireArgs(project);
            }
        }
    }

    private void injectSurefireArgs(MavenProject project) {
        Properties props = project.getProperties();
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Found quarkus-maven-plugin in " + project.getArtifactId() + ": inspecting for missing Surefire arguments");
        }
        final String originalArgLine = props.getProperty("argLine", "");
        String newArgLine = originalArgLine;
        //We need these parameters set in advance; N.B. this approach won't scale efficiently.
        newArgLine = possiblyInject(newArgLine, "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED");
        newArgLine = possiblyInject(newArgLine, "--add-opens=java.base/java.lang=ALL-UNNAMED");
        newArgLine = possiblyInject(newArgLine, "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED");
        newArgLine = newArgLine.trim(); // micro cleanup
        props.setProperty("argLine", newArgLine);
        if (logger.isDebugEnabled()) {
            logger.debug("Quarkus Maven extension: injected 'argLine' for Surefire: " + newArgLine);
        }
    }

    private String possiblyInject(final String existingArgLine, final String requiredJvmParameter) {
        if (!existingArgLine.contains(requiredJvmParameter)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Quarkus Maven extension: injecting JVM parameter '" + requiredJvmParameter + "' into Surefire");
            }
            return existingArgLine + " " + requiredJvmParameter;
        }
        return existingArgLine;
    }

    private boolean isQuarkusPluginConfigured(MavenProject project) {
        if (project.getBuild() == null || project.getBuild().getPlugins() == null) {
            return false;
        }
        for (Plugin plugin : project.getBuild().getPlugins()) {
            if ("quarkus-maven-plugin".equals(plugin.getArtifactId())) {
                return true; // it's enabled on this project
            }
        }
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
