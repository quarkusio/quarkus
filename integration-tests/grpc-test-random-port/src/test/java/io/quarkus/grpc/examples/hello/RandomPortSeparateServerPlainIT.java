package io.quarkus.grpc.examples.hello;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(RandomPortSeparateServerPlainTestBase.Profile.class)
class RandomPortSeparateServerPlainIT extends RandomPortSeparateServerPlainTestBase {
}
