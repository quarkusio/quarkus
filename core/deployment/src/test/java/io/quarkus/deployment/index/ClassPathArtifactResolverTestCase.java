package io.quarkus.deployment.index;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ClassPathArtifactResolverTestCase {

    private static final ClassPathArtifactResolver RESOLVER = new ClassPathArtifactResolver(
            ClassPathArtifactResolverTestCase.class.getClassLoader());

    @Test
    public void testSingleGroupArtifact() throws Exception {
        assertNotNull(RESOLVER.getArtifact("junit", "junit", null));
    }

    @Test
    public void testMultipleGroupArtifact() throws Exception {
        assertNotNull(RESOLVER.getArtifact("javax.annotation", "javax.annotation-api", null));
    }

    @Test(expected = RuntimeException.class)
    public void testClassifierNotFound() throws Exception {
        assertNotNull(RESOLVER.getArtifact("junit", "junit", "unknow-classifier"));
    }

}
