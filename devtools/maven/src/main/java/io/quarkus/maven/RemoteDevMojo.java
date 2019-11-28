package io.quarkus.maven;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.maven.components.MavenVersionEnforcer;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.remotedev.AgentRunner;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;

/**
 * The dev mojo, that connects to a remote host.
 */
@Mojo(name = "remote-dev", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class RemoteDevMojo extends AbstractMojo {

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    private File sourceDir;

    @Parameter(defaultValue = "${jvm.args}")
    private String jvmArgs;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Parameter(defaultValue = "TRUE")
    private boolean deleteDevJar;

    @Component
    private MavenVersionEnforcer mavenVersionEnforcer;

    @Component
    private ToolchainManager toolchainManager;

    public ToolchainManager getToolchainManager() {
        return toolchainManager;
    }

    public MavenSession getSession() {
        return session;
    }

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        mavenVersionEnforcer.ensureMavenVersion(getLog(), session);
        Plugin found = MojoUtils.checkProjectForMavenBuildPlugin(project);

        if (found == null) {
            getLog().warn("The quarkus-maven-plugin build goal was not configured for this project, " +
                    "skipping quarkus:remote-dev as this is assumed to be a support library. If you want to run Quarkus remote-dev"
                    +
                    " on this project make sure the quarkus-maven-plugin is configured with a build goal.");
            return;
        }

        if (!sourceDir.isDirectory()) {
            throw new MojoFailureException("The `src/main/java` directory is required, please create it.");
        }

        String resources = null;
        for (Resource i : project.getBuild().getResources()) {
            //todo: support multiple resources dirs for config hot deployment
            resources = i.getDirectory();
            break;
        }

        String classes = outputDirectory.getAbsolutePath();
        String sources = sourceDir.getAbsolutePath();

        //first lets look for some config, as it is not on the current class path
        //and we need to load it to run the build process
        if (resources != null) {
            Path config = Paths.get(resources).resolve("application.properties");
            if (Files.exists(config)) {
                try {
                    SmallRyeConfig built = ConfigUtils.configBuilder(false)
                            .withSources(new PropertiesConfigSource(config.toUri().toURL())).build();
                    QuarkusConfigFactory.setConfig(built);
                    final ConfigProviderResolver cpr = ConfigProviderResolver.instance();
                    final Config existing = cpr.getConfig();
                    if (existing != built) {
                        cpr.releaseConfig(existing);
                        // subsequent calls will get the new config
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Optional<String> url = ConfigProvider.getConfig().getOptionalValue("quarkus.live-reload.url", String.class);
        Optional<String> password = ConfigProvider.getConfig().getOptionalValue("quarkus.live-reload.password",
                String.class);
        if (!url.isPresent()) {
            throw new MojoFailureException("To use remote-dev you must specify quarkus.live-reload.url");
        }
        if (!password.isPresent()) {
            throw new MojoFailureException("To use remote-dev you must specify quarkus.live-reload.password");
        }
        System.out.println(sources);
        String remotePath = url.get();
        if (remotePath.endsWith("/")) {
            remotePath = remotePath.substring(0, remotePath.length() - 1);
        }
        AgentRunner runner = new AgentRunner(resources, sources, classes, remotePath + "/quarkus/live-reload",
                password.get());

        runner.run();
        for (;;) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
