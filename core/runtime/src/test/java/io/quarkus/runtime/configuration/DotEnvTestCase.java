package io.quarkus.runtime.configuration;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.DotEnvConfigSourceProvider;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 *
 */
public class DotEnvTestCase {

    private SmallRyeConfig buildConfig(Map<String, String> configMap) throws IOException {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        final Path dotEnv = Files.createTempFile("test-", ".env");
        try (OutputStream fos = Files.newOutputStream(dotEnv, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            try (OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                try (BufferedWriter bw = new BufferedWriter(osw)) {
                    for (Map.Entry<String, String> entry : configMap.entrySet()) {
                        bw.write(entry.getKey());
                        bw.write('=');
                        bw.write(entry.getValue());
                        bw.newLine();
                    }
                }
            }
        }
        builder.withSources(new DotEnvConfigSourceProvider(dotEnv.toUri().toString()));
        final SmallRyeConfig config = builder.build();
        Files.delete(dotEnv);
        return config;
    }

    private Map<String, String> maps(Map<String, String>... maps) {
        Map<String, String> out = new HashMap<>();
        for (Map<String, String> map : maps) {
            out.putAll(map);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProperties() throws IOException {
        final SmallRyeConfig config = buildConfig(
                maps(singletonMap("FOO_BAR", "foo.bar"), singletonMap("foo.baz", "nothing")));
        assertEquals("foo.bar", config.getValue("foo.bar", String.class));
        assertTrue(config.getOptionalValue("foo.baz", String.class).isPresent());
    }
}
