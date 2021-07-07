
package io.quarkus.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.deployment.util.ExecUtil;

public class IsDockerWorking implements BooleanSupplier {

    private static final Logger LOGGER = Logger.getLogger(IsDockerWorking.class.getName());

    private final boolean silent;

    public IsDockerWorking() {
        this(false);
    }

    public IsDockerWorking(boolean silent) {
        this.silent = silent;
    }

    @Override
    public boolean getAsBoolean() {
        //remote docker detection
        //we don't want to pull in the docker API here
        //so we just see if the DOCKER_HOST is set and we can connect to it
        //we can't actually verify it is docker listening on the other end
        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost != null) {
            try {
                URI url = new URI(dockerHost);
                try (Socket s = new Socket(url.getHost(), url.getPort())) {
                    return true;
                } catch (IOException e) {
                    LOGGER.warnf("Unable to connect to DOCKER_HOST URI %s, make sure docker is running on the specified host",
                            dockerHost);
                }
            } catch (URISyntaxException e) {
                LOGGER.warnf("Unable to parse DOCKER_HOST URI %s, it will be ignored for working docker detection", dockerHost);
            }
        }

        try {
            if (!ExecUtil.execSilent("docker", "-v")) {
                LOGGER.warn("'docker -v' returned an error code. Make sure your Docker binary is correct");
                return false;
            }
        } catch (Exception e) {
            LOGGER.warnf("No Docker binary found or general error: %s", e);
            return false;
        }

        try {
            OutputFilter filter = new OutputFilter();
            if (ExecUtil.exec(new File("."), filter, "docker", "version", "--format", "'{{.Server.Version}}'")) {
                LOGGER.debugf("Docker daemon found. Version: %s", filter.getOutput());
                return true;
            } else {
                if (!silent) {
                    LOGGER.warn("Could not determine version of Docker daemon");
                }
                return false;
            }
        } catch (Exception e) {
            LOGGER.warn("Unexpected error occurred while determining Docker daemon version", e);
            return false;
        }
    }

    public static class OutputFilter implements Function<InputStream, Runnable> {
        private final StringBuilder builder = new StringBuilder();

        @Override
        public Runnable apply(InputStream is) {
            return () -> {
                try (InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader reader = new BufferedReader(isr)) {

                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        builder.append(line);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error reading stream.", e);
                }
            };
        }

        public String getOutput() {
            return builder.toString();
        }
    }

    public static class IsDockerRunningSilent extends IsDockerWorking {
        public IsDockerRunningSilent() {
            super(true);
        }
    }
}
