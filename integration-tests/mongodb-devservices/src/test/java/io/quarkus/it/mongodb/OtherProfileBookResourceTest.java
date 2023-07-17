package io.quarkus.it.mongodb;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DummyTestProfile.class)
public class OtherProfileBookResourceTest {

    @Test
    public void testBlockingClient() {
        Utils.callTheEndpoint("/books");
    }
}
