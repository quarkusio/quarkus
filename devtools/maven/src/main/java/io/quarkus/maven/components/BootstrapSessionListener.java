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

    private final QuarkusBootstrapProvider bootstrapProvider;
    private final BuildAnalyticsProvider buildAnalyticsProvider;

    private boolean enabled;

    @Inject
    public BootstrapSessionListener(QuarkusBootstrapProvider bootstrapProvider, BuildAnalyticsProvider buildAnalyticsProvider) {
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
        logDebug(session, "Quarkus Maven extension: injecting JVM args for Surefire");
        for (MavenProject project : session.getProjects()) {
            if (isQuarkusPluginConfigured(project)) {
                logDebug(session,
                        "Found quarkus-maven-plugin in " + project.getArtifactId() + ": injecting Surefire arguments");
                injectSurefireArgs(session, project);
            }
        }
    }

    private void injectSurefireArgs(MavenSession session, MavenProject project) {
        Properties props = project.getProperties();
        String existingArgLine = props.getProperty("argLine", "");

        String jvmArgsAdd = "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED"
                + " --add-opens=java.base/java.lang=ALL-UNNAMED"
                + " --add-exports=java.base/jdk.internal.module=ALL-UNNAMED";
        String newArgLine = (existingArgLine.trim().isEmpty())
                ? jvmArgsAdd
                : jvmArgsAdd + existingArgLine;

        props.setProperty("argLine", newArgLine);
        logDebug(session, "Quarkus Maven extension: injected 'argLine' for Surefire: " + newArgLine);
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

    //Injecting the Logger is apparently error-prone, it might fail in various conditions.
    //It's tempting to rather not log at all, but let's have some output about this situation
    //by falling back to a regular error stream rather than being fully unaware.
    private void logDebug(MavenSession session, String message) {
        Logger logger = null;
        try {
            logger = (Logger) session.getContainer().lookup(Logger.class.getName());
        } catch (Exception e) {
            // Let's ignore the message in this case: while we don't want to fail just because
            // we wanted to print some diagnostics message doesn't imply that the message was important.
            // Not logging the message as it might confuse about its severity.
            System.err.println("[Quarkus Maven extension: failed Logger Lookup, debug logging will not be available]");
        }
        if (logger != null) {
            logger.debug(message);
        }
    }
}
