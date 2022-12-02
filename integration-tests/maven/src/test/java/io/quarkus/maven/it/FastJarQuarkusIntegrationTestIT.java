package io.quarkus.maven.it;

import java.io.IOException;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

@DisableForNative
public class FastJarQuarkusIntegrationTestIT extends QuarkusITBase {

    @Test
    public void testFastJar() throws MavenInvocationException, IOException {
        doTest("qit-fast-jar", "fastjar");
    }
}
