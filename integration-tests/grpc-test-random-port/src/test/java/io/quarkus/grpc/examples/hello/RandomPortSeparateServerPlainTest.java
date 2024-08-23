package io.quarkus.grpc.examples.hello;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(RandomPortSeparateServerPlainTestBase.Profile.class)
class RandomPortSeparateServerPlainTest extends RandomPortSeparateServerPlainTestBase {
}
