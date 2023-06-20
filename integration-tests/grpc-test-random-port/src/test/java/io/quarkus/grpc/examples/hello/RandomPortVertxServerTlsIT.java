package io.quarkus.grpc.examples.hello;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(RandomPortVertxServerTlsTestBase.Profile.class)
class RandomPortVertxServerTlsIT extends RandomPortVertxServerTlsTestBase {
}
