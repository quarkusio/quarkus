package io.quarkus.it.nat.test.profile;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(RuntimeProfileChangeIT.CustomTestProfile.class)
public class RuntimeProfileChangeIT extends RuntimeProfileChangeTest {
}
