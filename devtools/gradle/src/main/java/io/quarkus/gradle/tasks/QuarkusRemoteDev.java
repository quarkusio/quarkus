package io.quarkus.gradle.tasks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.gradle.QuarkusPluginExtension;
import io.quarkus.remotedev.AgentRunner;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;

public class QuarkusRemoteDev extends QuarkusTask {

    private static final String LIVE_RELOAD_URL = "quarkus.live-reload.url";
    private static final String LIVE_RELOAD_PASSWORD = "quarkus.live-reload.password";

    public QuarkusRemoteDev() {
        super("Remote development mode: enables hot deployment on remote JVM with background compilation");
    }

    @TaskAction
    public void startRemoteDev() {
        Project project = getProject();
        QuarkusPluginExtension extension = (QuarkusPluginExtension) project.getExtensions().findByName("quarkus");
        if (!extension.sourceDir().isDirectory()) {
            throw new GradleException("The `src/main/java` directory is required, please create it.");
        }
        if (extension.resourcesDir().isEmpty()) {
            throw new GradleException("The `src/main/resources` directory is required, please create it");
        }
        if (!extension.outputDirectory().isDirectory()) {
            throw new GradleException("The project has no output yet, " +
                    "this should not happen as build should have been executed first. " +
                    "Does the project have any source files?");
        }

        String classes = extension.outputDirectory().getAbsolutePath();
        String sources = extension.sourceDir().getAbsolutePath();

        String resources = null;
        for (File resource : extension.resourcesDir()) {
            resources = resource.getAbsolutePath();
            break;
        }

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

        Optional<String> url = ConfigProvider.getConfig().getOptionalValue(LIVE_RELOAD_URL, String.class);
        Optional<String> password = ConfigProvider.getConfig().getOptionalValue(LIVE_RELOAD_PASSWORD, String.class);
        if (!url.isPresent()) {
            throw new GradleException("To use remote-dev you must specify quarkus.live-reload.url");
        }
        if (!password.isPresent()) {
            throw new GradleException("To use remote-dev you must specify quarkus.live-reload.password");
        }

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
