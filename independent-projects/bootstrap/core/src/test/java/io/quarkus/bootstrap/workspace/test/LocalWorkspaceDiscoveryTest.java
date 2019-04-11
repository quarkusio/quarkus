/**
 *
 */
package io.quarkus.bootstrap.workspace.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;

public class LocalWorkspaceDiscoveryTest {

    @Test
    public void test() throws Exception {
        final LocalProject project = LocalProject.load(getResource("local-workspace/root/module1"));
        assertNotNull(project);
        assertNull(project.getWorkspace());
        assertEquals("org.acme", project.getGroupId());
    }

    private Path getResource(String relativePath) {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(relativePath);
        assertNotNull(url);
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to resolve path for " + url);
        }
    }
}
