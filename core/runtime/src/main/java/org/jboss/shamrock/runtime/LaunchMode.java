package org.jboss.shamrock.runtime;

public enum LaunchMode {
    /**
     * A normal production build. At the moment this can be both native image or
     * JVM mode, but eventually these will likely be split
     */
    NORMAL,
    /**
     * shamrock:dev or an IDE launch (when we support IDE launch)
     */
    DEVELOPMENT,
    /**
     * a test run
     */
    TEST
    ;

    public boolean isDevOrTest() {
        return this != NORMAL;
    }
}
