package io.quarkus.it.nat.test.profile;

import io.quarkus.test.junit.NativeImageTest;
import io.quarkus.test.junit.TestProfile;

@NativeImageTest
@TestProfile(RuntimeProfileChangeIT.CustomTestProfile.class)
public class RuntimeProfileChangeIT extends RuntimeProfileChangeTest {
}
