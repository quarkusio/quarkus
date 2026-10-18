package io.quarkus.it.nat.test.profile;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(RuntimeValueChangeIT.CustomTestProfile.class)
public class RuntimeValueChangeIT extends RuntimeValueChangeTest {
}
