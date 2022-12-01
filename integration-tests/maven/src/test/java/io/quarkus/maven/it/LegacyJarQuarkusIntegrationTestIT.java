package io.quarkus.maven.it;

import java.io.IOException;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

@DisableForNative
public class LegacyJarQuarkusIntegrationTestIT extends QuarkusITBase {

    @Test
    public void testLegacyJar() throws MavenInvocationException, IOException {
        doTest("qit-legacy-jar", "legacyjar");
    }
}
