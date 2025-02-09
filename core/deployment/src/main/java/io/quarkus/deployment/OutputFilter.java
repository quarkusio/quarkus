package io.quarkus.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Function;

import org.jboss.logging.Logger;

public class OutputFilter implements Function<InputStream, Runnable> {
    private final StringBuilder builder = new StringBuilder();
    private static final Logger log = Logger.getLogger(OutputFilter.class);

    @Override
    public Runnable apply(InputStream is) {
        return () -> {

            try (InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr)) {

                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    builder.append(line);
                }
            } catch (IOException e) {
                if (e.getMessage().contains("Stream closed")) {
                    log.warn("Stream is closed, ignoring and trying to continue");
                } else {
                    throw new RuntimeException("Error reading stream.", e);
                }
            }
        };
    }

    public String getOutput() {
        return builder.toString();
    }
}
