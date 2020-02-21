package io.quarkus.container.image.s2i.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.deployment.util.ExecUtil;

public class S2iBuild implements BooleanSupplier {

    private static final Logger LOGGER = Logger.getLogger(S2iBuild.class.getName());

    private ContainerImageConfig containerImageConfig;

    S2iBuild(ContainerImageConfig containerImageConfig) {
        this.containerImageConfig = containerImageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        OutputFilter filter = new OutputFilter();
        try {
            if (ExecUtil.exec(new File("."), filter, "oc", "version")) {
                Optional<String> version = getServerVersionFromOc(filter.getLines());
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static Optional<String> getServerVersionFromOc(List<String> lines) {
        return lines.stream()
                .filter(l -> l.startsWith("kubernetes"))
                .map(l -> l.split(" "))
                .filter(a -> a.length > 2)
                .map(a -> a[1])
                .findFirst();
    }

    private static class OutputFilter implements Function<InputStream, Runnable> {
        private final List<String> list = new ArrayList();

        @Override
        public Runnable apply(InputStream is) {
            return () -> {
                try (InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader reader = new BufferedReader(isr)) {

                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        list.add(line);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error reading stream.", e);
                }
            };
        }

        public List<String> getLines() {
            return list;
        }
    }
}
