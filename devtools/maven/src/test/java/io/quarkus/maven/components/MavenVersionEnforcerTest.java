package io.quarkus.maven.components;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Properties;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

class MavenVersionEnforcerTest {

    private final MavenVersionEnforcer enforcer = new MavenVersionEnforcer();

    @Test
    void unsupportedMavenVersionFailsTheBuild() {
        assertThatThrownBy(() -> enforcer.ensureMavenVersion(new SystemStreamLog(), session("3.0.5"), false))
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("(3.0.5)").hasMessageContaining("is not supported");
    }

    @Test
    void unsupportedMavenVersionIsAcceptedWhenTheCheckIsSkipped() {
        assertThatCode(() -> enforcer.ensureMavenVersion(new SystemStreamLog(), session("3.0.5"), true))
                .doesNotThrowAnyException();
    }

    private static MavenSession session(String mavenVersion) {
        Properties systemProperties = new Properties();
        systemProperties.setProperty("maven.version", mavenVersion);
        DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setSystemProperties(systemProperties);
        return new MavenSession(null, null, request, new DefaultMavenExecutionResult());
    }
}
