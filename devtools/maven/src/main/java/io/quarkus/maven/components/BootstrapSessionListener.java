package io.quarkus.maven.components;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import io.quarkus.maven.BuildAnalyticsProvider;
import io.quarkus.maven.QuarkusBootstrapProvider;

@SessionScoped
@Named("quarkus-bootstrap")
public class BootstrapSessionListener extends AbstractMavenLifecycleParticipant {

    private static final List<String> INJECTED_JVM_PROPS = List.of(
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED");

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
        logger.debug("Quarkus Maven extension: inspecting Surefire and Failsafe configurations");
        for (MavenProject project : session.getProjects()) {
            //Let's not interfere with non-Quarkus modules:
            if (isQuarkusPluginConfigured(project)) {
                injectSurefireArgs(project);
            }
        }
    }

    private void injectSurefireArgs(MavenProject project) {
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Found quarkus-maven-plugin in " + project.getArtifactId()
                            + ": inspecting for missing Surefire and Failsafe arguments");
        }

        // Process Surefire and Failsafe plugins, for both of them we want to inject the JVM parameters that Quarkus will need:
        injectArgsForPlugin(project, "maven-surefire-plugin");
        injectArgsForPlugin(project, "maven-failsafe-plugin");
    }

    private void injectArgsForPlugin(MavenProject project, String pluginArtifactId) {
        Properties props = project.getProperties();

        // Find the plugin and check for explicit argLine configuration
        Plugin plugin = findPlugin(project, pluginArtifactId);
        if (plugin == null) {
            // Plugin not configured in this project
            return;
        }

        Object configuration = plugin.getConfiguration();
        Xpp3Dom configDom = (configuration instanceof Xpp3Dom) ? (Xpp3Dom) configuration : null;
        Xpp3Dom argLineDom = (configDom != null) ? configDom.getChild("argLine") : null;

        final boolean hasExplicitConfig = (argLineDom != null && argLineDom.getValue() != null);
        final String userDefinedArgLine = props.getProperty("argLine", "").trim();

        if (hasExplicitConfig) {
            final String standardMarker = "@{argLine}";
            // Plugin has explicit argLine in its configuration block
            String explicitValue = argLineDom.getValue();
            if (logger.isDebugEnabled()) {
                logger.debug("Quarkus Maven extension: found explicit argLine configuration in " + pluginArtifactId
                        + " with value: '"
                        + explicitValue + "'");
            }
            final boolean standardMarkerIsPresent = explicitValue.contains(standardMarker);
            if (!standardMarkerIsPresent) {
                //If we have an explicit configuration, and yet the marker is not present, we're stuck and can't
                //inject anything into Surefire as it will ignore the property.
                //Suggest the user to insert the marker:
                logger.warn("Quarkus Maven extension: an explicit 'argLine' is defined in " + pluginArtifactId
                        + " config, but does not contain '@{argLine}': " +
                        "we will not be able to inject JVM parameters automatically. Please add '@{argLine}' to the argLine configuration you have defined.");
            }
            //N.B. it's tempting here to check if the JVM properties we need have been set already, to omit nagging the user with the warning when
            // the injection is not necessary, but not having "argLine" injection capabilities has implications on other integrations as well;
            // for example the jacoco integration will rely on it, and other Quarkus components will rely on it as well.
            //So: KISS and nudge to user towards this pattern rather than trying to handle too many options and cases dynamically.
        }

        //Inject the new values we need:
        final String newValue = generateNewArgLine(userDefinedArgLine);
        if (!newValue.equals(userDefinedArgLine)) {
            props.setProperty("argLine", newValue);
            if (logger.isDebugEnabled()) {
                logger.debug("Quarkus Maven extension: set project property 'argLine' to: '" + newValue + "'");
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Quarkus Maven extension: not setting 'argLine' as its content doesn't need tuning.");
            }
        }
    }

    private String generateNewArgLine(String userDefinedArgLine) {
        StringBuilder sb = new StringBuilder();

        // Start with the injected properties, avoiding duplicates if they already exist in userDefinedArgLine:
        // Duplicates are very likely as this injection process runs twice if both Surefire and Failsafe are configured!
        for (String prop : INJECTED_JVM_PROPS) {
            if (!userDefinedArgLine.contains(prop)) {
                sb.append(prop);
                sb.append(" ");
            }
        }
        // Finally, append the user defined argLine:
        sb.append(userDefinedArgLine);
        return sb.toString().trim();
    }

    private Plugin findPlugin(MavenProject project, String pluginArtifactId) {
        if (project.getBuild() == null || project.getBuild().getPlugins() == null) {
            return null;
        }
        for (Plugin plugin : project.getBuild().getPlugins()) {
            if (pluginArtifactId.equals(plugin.getArtifactId())) {
                return plugin;
            }
        }
        return null;
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
