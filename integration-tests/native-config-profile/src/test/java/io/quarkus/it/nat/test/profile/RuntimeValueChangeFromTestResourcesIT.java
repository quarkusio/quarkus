package io.quarkus.it.nat.test.profile;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.NativeImageTest;

/**
 * This test ensures that the NativeTestExtension starts the test resources from the Test Profile annotation.
 */
@NativeImageTest
public class RuntimeValueChangeFromTestResourcesIT extends RuntimeValueChangeFromTestResourcesTest {
}
