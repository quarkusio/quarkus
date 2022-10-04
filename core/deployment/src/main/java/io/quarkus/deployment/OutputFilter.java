package io.quarkus.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Function;

public class OutputFilter implements Function<InputStream, Runnable> {
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
