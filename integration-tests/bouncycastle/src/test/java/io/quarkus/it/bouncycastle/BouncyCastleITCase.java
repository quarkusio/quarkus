package io.quarkus.it.bouncycastle;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class BouncyCastleITCase extends BouncyCastleTestCase {
    @Test
    public void testJceKS() throws Exception {
        super.doTestJceKS();
    }
}
