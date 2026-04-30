package io.quarkus.maven.components;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import io.quarkus.deployment.dev.AnnotationProcessorPaths;
import io.quarkus.deployment.dev.AnnotationProcessorProvider;
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

        // Discover and inject annotation processors from extensions
        discoverAndInjectAnnotationProcessors(session);
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

    private void discoverAndInjectAnnotationProcessors(MavenSession session) {
        RepositorySystem repoSystem = bootstrapProvider.repositorySystem();

        for (MavenProject project : session.getProjects()) {
            // Only process Quarkus projects
            if (!isQuarkusPluginConfigured(project)) {
                continue;
            }

            try {
                // Resolve project dependencies (compile + runtime scope)
                List<URL> urls = resolveDependencies(project, session, repoSystem);

                logger.info("Discovering annotation processors for " + project.getArtifactId() +
                        " with " + urls.size() + " resolved JARs");

                // Discover annotation processors via SPI
                try (URLClassLoader extensionClassLoader = new URLClassLoader(
                        urls.toArray(new URL[0]),
                        getClass().getClassLoader())) {

                    ServiceLoader<AnnotationProcessorProvider> providers = ServiceLoader.load(
                            AnnotationProcessorProvider.class, extensionClassLoader);

                    Set<String> processors = new LinkedHashSet<>();
                    for (AnnotationProcessorProvider provider : providers) {
                        processors.addAll(provider.getAnnotationProcessors());
                        String extensionCoord = AnnotationProcessorPaths.getExtensionCoordinate(provider);
                        logger.info("Extension " + extensionCoord +
                                " requires processors: " + provider.getAnnotationProcessors());
                    }

                    if (!processors.isEmpty()) {
                        injectAnnotationProcessorsIntoCompiler(project, processors);
                    }
                }

            } catch (Exception e) {
                logger.warn("Failed to discover annotation processors for " +
                        project.getArtifactId() + ": " + e.getMessage(), e);
            }
        }
    }

    private List<URL> resolveDependencies(MavenProject project, MavenSession session,
            RepositorySystem repoSystem) throws Exception {
        List<URL> urls = new ArrayList<>();

        // Build collect request from project dependencies
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRepositories(project.getRemoteProjectRepositories());

        for (org.apache.maven.model.Dependency dep : project.getDependencies()) {
            // Skip test-scoped dependencies
            if ("test".equals(dep.getScope())) {
                continue;
            }

            String coords = dep.getGroupId() + ":" + dep.getArtifactId() + ":"
                    + (dep.getVersion() != null ? dep.getVersion() : "");
            Artifact artifact = new DefaultArtifact(coords);
            collectRequest.addDependency(new Dependency(artifact, dep.getScope()));

            // For Quarkus runtime artifacts, also add corresponding deployment module
            if ("io.quarkus".equals(dep.getGroupId()) &&
                    dep.getArtifactId().startsWith("quarkus-") &&
                    !dep.getArtifactId().endsWith("-deployment")) {

                String deploymentArtifactId = dep.getArtifactId() + "-deployment";
                String deploymentCoords = dep.getGroupId() + ":" + deploymentArtifactId + ":"
                        + (dep.getVersion() != null ? dep.getVersion() : "");
                Artifact deploymentArtifact = new DefaultArtifact(deploymentCoords);
                collectRequest.addDependency(new Dependency(deploymentArtifact, JavaScopes.COMPILE));

                logger.debug("Added deployment dependency: " + deploymentCoords);
            }
        }

        // Resolve dependencies
        DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE, JavaScopes.RUNTIME);
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);

        DependencyResult result = repoSystem.resolveDependencies(
                session.getRepositorySession(), dependencyRequest);

        // Collect resolved artifact files
        for (ArtifactResult ar : result.getArtifactResults()) {
            if (ar.getArtifact().getFile() != null) {
                urls.add(ar.getArtifact().getFile().toURI().toURL());
            }
        }

        return urls;
    }

    private void injectAnnotationProcessorsIntoCompiler(MavenProject project, Set<String> processors) {
        Plugin compilerPlugin = findPlugin(project, "maven-compiler-plugin");
        if (compilerPlugin == null) {
            logger.debug("No maven-compiler-plugin configured for " + project.getArtifactId() +
                    ", skipping annotation processor injection");
            return;
        }

        // Set on plugin-level configuration
        addProcessorsToConfiguration(compilerPlugin, processors, project);

        // Also set on all executions
        for (PluginExecution execution : compilerPlugin.getExecutions()) {
            addProcessorsToConfiguration(execution, processors, project);
        }
    }

    private void addProcessorsToConfiguration(Object configHolder, Set<String> processors, MavenProject project) {
        Xpp3Dom configuration;
        if (configHolder instanceof Plugin) {
            configuration = (Xpp3Dom) ((Plugin) configHolder).getConfiguration();
            if (configuration == null) {
                configuration = new Xpp3Dom("configuration");
                ((Plugin) configHolder).setConfiguration(configuration);
            }
        } else if (configHolder instanceof PluginExecution) {
            configuration = (Xpp3Dom) ((PluginExecution) configHolder).getConfiguration();
            if (configuration == null) {
                configuration = new Xpp3Dom("configuration");
                ((PluginExecution) configHolder).setConfiguration(configuration);
            }
        } else {
            return;
        }

        // Get or create annotationProcessorPaths
        Xpp3Dom processorPaths = configuration.getChild("annotationProcessorPaths");
        if (processorPaths == null) {
            processorPaths = new Xpp3Dom("annotationProcessorPaths");
            configuration.addChild(processorPaths);
        }

        // Collect already configured processors to avoid duplicates
        Set<String> existing = new HashSet<>();
        for (Xpp3Dom path : processorPaths.getChildren("path")) {
            Xpp3Dom groupId = path.getChild("groupId");
            Xpp3Dom artifactId = path.getChild("artifactId");
            if (groupId != null && artifactId != null) {
                existing.add(groupId.getValue() + ":" + artifactId.getValue());
            }
        }

        // Add new processors
        for (String processor : processors) {
            String[] parts = processor.split(":");
            if (parts.length >= 2) {
                String coord = parts[0] + ":" + parts[1];

                if (!existing.contains(coord)) {
                    Xpp3Dom path = new Xpp3Dom("path");
                    Xpp3Dom groupId = new Xpp3Dom("groupId");
                    groupId.setValue(parts[0]);
                    Xpp3Dom artifactId = new Xpp3Dom("artifactId");
                    artifactId.setValue(parts[1]);
                    path.addChild(groupId);
                    path.addChild(artifactId);

                    // Optional version (if provided)
                    if (parts.length >= 3) {
                        Xpp3Dom version = new Xpp3Dom("version");
                        version.setValue(parts[2]);
                        path.addChild(version);
                    }

                    processorPaths.addChild(path);
                    logger.info("Auto-configured annotation processor: " + processor +
                            " for " + project.getArtifactId());
                }
            }
        }

        // Enable dependency management for processor versions
        Xpp3Dom useDepMgmt = configuration.getChild("annotationProcessorPathsUseDepMgmt");
        if (useDepMgmt == null) {
            useDepMgmt = new Xpp3Dom("annotationProcessorPathsUseDepMgmt");
            useDepMgmt.setValue("true");
            configuration.addChild(useDepMgmt);
        }

        // Ensure annotation processing is NOT disabled (proc should be "only" or not set)
        Xpp3Dom proc = configuration.getChild("proc");
        if (proc != null && "none".equals(proc.getValue())) {
            logger.warn("Annotation processing is disabled (proc=none) for " + project.getArtifactId() +
                    ", cannot auto-configure processors");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
