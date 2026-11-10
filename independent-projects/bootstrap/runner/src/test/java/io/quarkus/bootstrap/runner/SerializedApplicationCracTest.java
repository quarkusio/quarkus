package io.quarkus.bootstrap.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SerializedApplicationCracTest {

    @TempDir
    Path appRoot;

    @Test
    public void testCracEnabledAtBuildTime() throws IOException {
        assertThat(read(write(true)).isCracResourceRegistered()).isTrue();
    }

    @Test
    public void testCracDisabledAtBuildTime() throws IOException {
        assertThat(read(write(false)).isCracResourceRegistered()).isFalse();
    }

    @Test
    public void testCracDisabledByDefault() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SerializedApplication.write(out, "org.acme.Main", appRoot, List.of(), List.of());
        assertThat(read(out.toByteArray()).isCracResourceRegistered()).isFalse();
    }

    @Test
    public void testCracEnabledBySystemProperty() throws IOException {
        System.setProperty(CracSupport.ENABLED_PROPERTY, "true");
        try {
            assertThat(read(write(false)).isCracResourceRegistered()).isTrue();
        } finally {
            System.clearProperty(CracSupport.ENABLED_PROPERTY);
        }
    }

    private byte[] write(boolean cracEnabled) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SerializedApplication.write(out, "org.acme.Main", appRoot, List.of(), List.of(), cracEnabled);
        return out.toByteArray();
    }

    private RunnerClassLoader read(byte[] data) throws IOException {
        return SerializedApplication.read(new ByteArrayInputStream(data), appRoot).getRunnerClassLoader();
    }
}
