package io.quarkus.grpc.examples.hello;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(RandomPortVertxServerPlainTestBase.Profile.class)
class RandomPortVertxServerPlainTest extends RandomPortVertxServerPlainTestBase {
}
