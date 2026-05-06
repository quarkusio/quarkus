package io.quarkus.quickcli.deployment;

import static io.quarkus.quickcli.deployment.TestUtils.createConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Verifies that parsed CLI options are available as SmallRye Config properties.
 */
public class ConfigSourceTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("config-source-app", ConfigSourceCommand.class)
            .setCommandLineParameters("--server.port", "8080", "--app.name", "MyApp");

    @Test
    public void testCliOptionsAvailableAsConfigProperties() {
        String output = config.getStartupConsoleOutput();
        assertThat(output).contains("option.port=8080");
        assertThat(output).contains("config.server.port=8080");
        assertThat(output).contains("option.appName=MyApp");
        assertThat(output).contains("config.app.name=MyApp");
        assertThat(config.getExitCode()).isZero();
    }
}
