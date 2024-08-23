package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This makes sure that exclusions in POM files of deployment modules are respected.
 * <p>
 * One case where this is critical is when using quarkus-hibernate-reactive. Its deployment
 * module takes care of pulling in quarkus-hibernate-orm-deployment where it excludes
 * Agroal and Narayana.
 * <p>
 * If that exclusion isn't taken into account by the Gradle plugin, it pulls in
 * quarkus-hibernate-orm-deployment on its own, where those other modules aren't excluded,
 * which leads to a failure at runtime because Agroal tries to initialize and looks
 * for a JDBC driver which isn't typically available in reactive DB scenarios.
 */
public class MavenExclusionInExtensionDependencyDevModeTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "maven-exclusion-in-extension-dependency";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("Hello");
    }
}
