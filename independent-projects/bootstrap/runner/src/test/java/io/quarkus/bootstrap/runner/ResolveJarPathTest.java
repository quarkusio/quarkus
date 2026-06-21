package io.quarkus.bootstrap.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class ResolveJarPathTest {

    @Test
    public void plusInPathIsPreserved() throws Exception {
        URL location = new URI("file:///opt/packages/myapp/1.0.0+build123/quarkus-app/quarkus-run.jar").toURL();

        Path resolved = QuarkusEntryPoint.resolveJarPath(location, null);

        assertThat(resolved).isEqualTo(Path.of("/opt/packages/myapp/1.0.0+build123/quarkus-app/quarkus-run.jar"));
    }

    @Test
    public void spacesInPathAreDecoded() throws Exception {
        URL location = new URI("file:///opt/my%20app/quarkus-app/quarkus-run.jar").toURL();

        Path resolved = QuarkusEntryPoint.resolveJarPath(location, null);

        assertThat(resolved).isEqualTo(Path.of("/opt/my app/quarkus-app/quarkus-run.jar"));
    }

    @Test
    public void fallbackJarUrlPreservesPlus() throws Exception {
        URL classResource = new URI(
                "jar:file:///opt/packages/1.0.0+build123/quarkus-app/quarkus-run.jar!/io/quarkus/bootstrap/runner/QuarkusEntryPoint.class")
                .toURL();

        Path resolved = QuarkusEntryPoint.resolveJarPath(null, classResource);

        assertThat(resolved).isEqualTo(Path.of("/opt/packages/1.0.0+build123/quarkus-app/quarkus-run.jar"));
    }

    @Test
    public void fallbackJarUrlDecodesSpaces() throws Exception {
        URL classResource = new URI(
                "jar:file:///opt/my%20app/quarkus-run.jar!/io/quarkus/bootstrap/runner/QuarkusEntryPoint.class")
                .toURL();

        Path resolved = QuarkusEntryPoint.resolveJarPath(null, classResource);

        assertThat(resolved).isEqualTo(Path.of("/opt/my app/quarkus-run.jar"));
    }

    @Test
    public void returnsNullWhenBothArgumentsAreNull() throws URISyntaxException {
        assertThat(QuarkusEntryPoint.resolveJarPath(null, null)).isNull();
    }
}
