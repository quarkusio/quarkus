package io.quarkus.maven.it;

import java.io.IOException;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisableForNative
@DisabledOnOs(OS.WINDOWS)
public class QuestionMarkInPathIntegrationTestIT extends QuarkusITBase {

    @Test
    public void testFastJar() throws MavenInvocationException, IOException {
        doTest("qit?fast?jar", "fastjar");
    }

    @Test
    public void testLegacyJar() throws MavenInvocationException, IOException {
        doTest("qit?legacy?jar", "legacyjar");
    }

    @Test
    public void testUberJar() throws MavenInvocationException, IOException {
        doTest("qit?uber?jar", "uberjar");
    }
}
