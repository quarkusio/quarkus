package io.quarkus.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.quickcli.ExitCode;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class NotRegisteredExtensionWithCodestartTest extends RegistryClientBuilderTestBase {

    @BeforeAll
    static void configureRegistryAndMavenRepo() {
        TestRegistryClientBuilder.newInstance()
                .baseDir(registryConfigDir())
                .newRegistry("registry.acme.org")
                .newPlatform("org.acme.quarkus.platform")
                .newStream("2.0")
                .newRelease("2.0.0")
                .quarkusVersion(getCurrentQuarkusVersion())
                .addCoreMember()
                .alignPluginsOnQuarkusVersion()
                .addDefaultCodestartExtensions()
                .registry()
                .clientBuilder()
                .addExternalExtensionWithCodestart("org.acme.quarkus", "acme-outlaw", "6.6.6")
                .clientBuilder()
                .build();
    }

    @Test
    void test(QuarkusMainLauncher launcher) throws Exception {
        this.currentLauncher = launcher;
        final CliDriver.Result createResult = run(workDir(), "create", "app", "acme-outlaw-codestart",
                "-x org.acme.quarkus:acme-outlaw:6.6.6");
        assertThat(createResult.exitCode).isEqualTo(ExitCode.OK)
                .as(() -> "Expected OK return code." + createResult);
        assertThat(createResult.stdout).contains("SUCCESS")
                .as(() -> "Expected confirmation that the project has been created." + createResult);

        final Path acmeOutlawJava = workDir().resolve("acme-outlaw-codestart")
                .resolve("src/main/java/org/acme/AcmeOutlaw.java");
        assertThat(acmeOutlawJava).exists();
    }
}
