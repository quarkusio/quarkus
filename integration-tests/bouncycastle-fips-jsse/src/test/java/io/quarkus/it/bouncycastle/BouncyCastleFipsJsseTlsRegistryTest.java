package io.quarkus.it.bouncycastle;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(TlsRegistryProfile.class)
public class BouncyCastleFipsJsseTlsRegistryTest extends BouncyCastleFipsJsseTestCase {
}
