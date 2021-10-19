package io.quarkus.it.bouncycastle;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.DisabledOnNativeImage;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class BouncyCastleJsseITCase extends BouncyCastleJsseTestCase {
    @Test
    @DisabledOnNativeImage
    @Override
    public void testListProviders() {
        doTestListProviders();
        checkLog(true);
    }
}
