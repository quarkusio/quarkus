package io.quarkus.it.bouncycastle;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(HttpSslProfile.class)
public class BouncyCastleFipsJsseITCase extends BouncyCastleFipsJsseTestCase {

    @Test
    @DisabledOnIntegrationTest
    @Override
    public void testListProviders() throws Exception {
        doTestListProviders();
        checkLog(true);
    }
}
