package io.quarkus.bootstrap.classloading;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ClassPathResourceIndexTestCase {

    @Test
    public void testGetResourceKey() {
        assertEquals("io/quarkus/core/deployment",
                ClassPathResourceIndex.getResourceKey("io/quarkus/core/deployment/MyClass.class"));
        assertEquals("io/quarkus/core/deployment",
                ClassPathResourceIndex.getResourceKey("io/quarkus/core/deployment/package/MyClass.class"));
        assertEquals("org/apache/commons",
                ClassPathResourceIndex.getResourceKey("org/apache/commons/codec/MyClass.class"));
        assertEquals("test.properties", ClassPathResourceIndex.getResourceKey("test.properties"));
        assertEquals("META-INF/maven/", ClassPathResourceIndex.getResourceKey("META-INF/maven/commons-codec/file.properties"));
        assertEquals("io/quarkus", ClassPathResourceIndex.getResourceKey("io/quarkus/MyClass.class"));
        assertEquals("META-INF/services/my-service", ClassPathResourceIndex.getResourceKey("META-INF/services/my-service"));
        assertEquals("META-INF/versions/17/io/quarkus/core",
                ClassPathResourceIndex.getResourceKey("META-INF/versions/17/io/quarkus/core/deployment"));
    }
}
