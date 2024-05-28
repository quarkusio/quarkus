package io.quarkus.hibernate.orm;

public class TestTags {
    /**
     * Tag for tests that use {@link io.quarkus.test.QuarkusDevModeTest},
     * so that surefire config can run them in a different execution
     * and keep the metaspace memory leaks there.
     */
    public static final String DEVMODE = "devmode";
}
