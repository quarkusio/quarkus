package io.quarkus.hibernate.orm;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class H2Util {
    private H2Util() {
    }

    public static String getActualVersion() {
        String actualVersion = System.getProperty("h2.version");
        assertThat(actualVersion)
                .as("h2.version system property should be set by maven-surefire-plugin")
                .isNotNull();
        return actualVersion;
    }
}
