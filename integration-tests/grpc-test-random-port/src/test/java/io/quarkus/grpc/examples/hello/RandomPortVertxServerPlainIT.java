package io.quarkus.grpc.examples.hello;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(RandomPortVertxServerPlainTestBase.Profile.class)
class RandomPortVertxServerPlainIT extends RandomPortVertxServerPlainTestBase {
}
