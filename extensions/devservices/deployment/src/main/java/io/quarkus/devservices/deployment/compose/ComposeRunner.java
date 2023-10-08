package io.quarkus.devservices.deployment.compose;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.dockerclient.TransportConfig;
import org.testcontainers.utility.CommandLine;

import io.quarkus.deployment.util.ExecUtil;

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
    private Map<String, String> env;
    private List<String> profiles;

    public ComposeRunner(String composeExecutable, List<File> composeFiles, String projectName) {
        this.composeExecutable = composeExecutable;
        this.composeFiles = composeFiles;
        this.identifier = projectName;
        this.cmd = "";
        this.env = Collections.emptyMap();
        this.profiles = Collections.emptyList();
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
        this.env = Collections.unmodifiableMap(env);
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

        final Map<String, String> environment = new HashMap<>(env);
        environment.put(PROJECT_NAME_ENV, identifier);

        TransportConfig transportConfig = DockerClientFactory.instance().getTransportConfig();
        String certPath = System.getenv(DOCKER_CERT_PATH_ENV);
        if (certPath != null) {
            environment.put(DOCKER_CERT_PATH_ENV, certPath);
            environment.put(DOCKER_TLS_VERIFY_ENV, "true");
        }
        environment.put(DOCKER_HOST_ENV, transportConfig.getDockerHost().toString());
        environment.put(COMPOSE_FILE_ENV, composeFiles
                .stream()
                .map(File::getAbsolutePath)
                .map(Objects::toString)
                .collect(Collectors.joining(File.pathSeparator)));
        if (!profiles.isEmpty()) {
            environment.put(COMPOSE_PROFILES_ENV, String.join(",", this.profiles));
        }

        LOG.debugv("Compose is running with env {0}", environment);
        LOG.infov("Compose is running command: {0} {1}", composeExecutable, cmd);

        try {
            final File pwd = composeFiles.get(0).getAbsoluteFile().getParentFile().getAbsoluteFile();
            int exitCode = ExecUtil.execProcess(
                    ExecUtil.startProcess(pwd, environment, composeExecutable, cmd.split("\\s+")),
                    is -> new ExecUtil.HandleOutput(is, Logger.Level.INFO, LOG));
            if (exitCode == 0) {
                LOG.info("Compose has finished running");
            } else {
                throw new RuntimeException("Compose exited abnormally with code " + exitCode +
                        " whilst running command: " +
                        composeExecutable + " " + cmd + ", with env vars: " + environment);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error running Compose command: " + cmd, e);
        }
    }

}
