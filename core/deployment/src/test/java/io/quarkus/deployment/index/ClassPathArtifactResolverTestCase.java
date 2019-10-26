package io.quarkus.deployment.index;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ClassPathArtifactResolverTestCase {

    private static final ClassPathArtifactResolver RESOLVER = new ClassPathArtifactResolver(
            ClassPathArtifactResolverTestCase.class.getClassLoader());

    @Test
    public void testSingleGroupArtifact() throws Exception {
        assertNotNull(RESOLVER.getArtifact("org.junit.jupiter", "junit-jupiter", null));
    }

    @Test
    public void testMultipleGroupArtifact() throws Exception {
        assertNotNull(RESOLVER.getArtifact("jakarta.annotation", "jakarta.annotation-api", null));
    }

    @Test
    public void testClassifierNotFound() throws Exception {
        assertThrows(RuntimeException.class,
                () -> RESOLVER.getArtifact("org.junit.jupiter", "junit-jupiter", "unknow-classifier"));
    }

}
