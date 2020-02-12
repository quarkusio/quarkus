
package io.quarkus.container.image.docker.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.deployment.util.ExecUtil;

public class DockerBuild implements BooleanSupplier {

    private static final Logger LOGGER = Logger.getLogger(DockerBuild.class.getName());
    private static boolean daemonFound = false;

    private final ContainerImageConfig containerImageConfig;

    public DockerBuild(ContainerImageConfig containerImageConfig) {
        this.containerImageConfig = containerImageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        // No need to perform the check multiple times.
        if (daemonFound) {
            return true;
        }
        try {
            OutputFilter filter = new OutputFilter();
            if (ExecUtil.exec(new File("."), filter, "docker", "version", "--format", "'{{.Server.Version}}'")) {
                LOGGER.info("Docker daemon found! Version:" + filter.getOutput());
                daemonFound = true;
                return true;
            } else {
                LOGGER.warn("Could not connect to docker daemon!");
                return false;
            }
        } catch (Exception e) {
            LOGGER.warn("Could not connect to docker daemon!");
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
}
