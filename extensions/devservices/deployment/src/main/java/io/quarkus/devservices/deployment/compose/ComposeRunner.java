package io.quarkus.devservices.deployment.compose;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.dockerclient.TransportConfig;
import org.testcontainers.utility.CommandLine;

import io.smallrye.common.process.ProcessBuilder;

/**
 * A class that runs compose commands.
 */
public class ComposeRunner {

    private static final Logger LOG = Logger.getLogger(ComposeRunner.class);

    private static final String DOCKER_HOST_ENV = "DOCKER_HOST";
    private static final String DOCKER_CERT_PATH_ENV = "DOCKER_CERT_PATH";
    private static final String DOCKER_TLS_VERIFY_ENV = "DOCKER_TLS_VERIFY";
    private static final String PROJECT_NAME_ENV = "COMPOSE_PROJECT_NAME";
    private static final String COMPOSE_FILE_ENV = "COMPOSE_FILE";
    private static final String COMPOSE_PROFILES_ENV = "COMPOSE_PROFILES";

    private final List<File> composeFiles;
    private final String identifier;
    private final String composeExecutable;
    private String cmd;
    private Map<String, String> additionalEnvVars;
    private List<String> profiles;

    public ComposeRunner(String composeExecutable, List<File> composeFiles, String projectName) {
        this.composeExecutable = composeExecutable;
        this.composeFiles = composeFiles;
        this.identifier = projectName;
        this.cmd = "";
        this.additionalEnvVars = Map.of();
        this.profiles = List.of();
    }

    /**
     * Set the compose command and args to run.
     *
     * @param cmd the command to run
     * @return this
     */
    public ComposeRunner withCommand(String cmd) {
        this.cmd = cmd;
        return this;
    }

    /**
     * Set the environment variables to use when running the command.
     *
     * @param env the environment variables
     * @return this
     */
    public ComposeRunner withEnv(Map<String, String> env) {
        this.additionalEnvVars = Collections.unmodifiableMap(env);
        return this;
    }

    /**
     * Set the profiles to use when running the command.
     *
     * @param profiles the profiles
     * @return this
     */
    public ComposeRunner withProfiles(List<String> profiles) {
        this.profiles = Collections.unmodifiableList(profiles);
        return this;
    }

    /**
     * Run the compose command.
     */
    public void run() {
        if (!CommandLine.executableExists(composeExecutable)) {
            throw new RuntimeException("Docker Compose not found. Is " + composeExecutable + " on the PATH?");
        }

        if (composeFiles.isEmpty()) {
            LOG.info("No compose files specified");
            return;
        }

        LOG.infov("Compose is running command: {0} {1}", composeExecutable, cmd);

        final File pwd = composeFiles.get(0).getAbsoluteFile().getParentFile().getAbsoluteFile();

        ProcessBuilder.newBuilder(composeExecutable)
                .directory(pwd.toPath())
                .arguments(cmd.split("\\s+"))
                .modifyEnvironment(env -> {
                    env.putAll(additionalEnvVars);

                    env.put(PROJECT_NAME_ENV, identifier);

                    TransportConfig transportConfig = DockerClientFactory.instance().getTransportConfig();
                    String certPath = System.getenv(DOCKER_CERT_PATH_ENV);
                    if (certPath != null) {
                        env.put(DOCKER_CERT_PATH_ENV, certPath);
                        env.put(DOCKER_TLS_VERIFY_ENV, "true");
                    }
                    env.put(DOCKER_HOST_ENV, transportConfig.getDockerHost().toString());
                    env.put(COMPOSE_FILE_ENV, composeFiles
                            .stream()
                            .map(File::getAbsolutePath)
                            .map(Objects::toString)
                            .collect(Collectors.joining(File.pathSeparator)));
                    if (!profiles.isEmpty()) {
                        env.put(COMPOSE_PROFILES_ENV, String.join(",", this.profiles));
                    }
                })
                .output().consumeLinesWith(8192, LOG::info)
                .error().logOnSuccess(false).consumeLinesWith(8192, LOG::info)
                .run();

        LOG.info("Compose has finished running");
    }

}
