package io.quarkus.it.bouncycastle;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(HttpSslProfile.class)
public class BouncyCastleFipsJsseHttpSslTest extends BouncyCastleFipsJsseTestCase {
}
