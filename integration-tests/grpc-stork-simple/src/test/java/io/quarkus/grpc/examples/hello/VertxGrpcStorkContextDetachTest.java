package io.quarkus.grpc.examples.hello;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(VertxGrpcStorkContextDetachTestProfile.class)
class VertxGrpcStorkContextDetachTest extends GrpcStorkContextDetachTestBase {

}
