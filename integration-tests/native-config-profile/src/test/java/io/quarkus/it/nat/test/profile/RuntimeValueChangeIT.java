package io.quarkus.it.nat.test.profile;

import io.quarkus.test.junit.NativeImageTest;
import io.quarkus.test.junit.TestProfile;

@NativeImageTest
@TestProfile(RuntimeValueChangeIT.CustomTestProfile.class)
public class RuntimeValueChangeIT extends RuntimeValueChangeTest {
}
