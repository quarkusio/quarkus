package io.quarkus.redis.devservices.it;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.devservices.it.profiles.DevServicesCustomPortReusableServiceProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.ports.SocketKit;

@QuarkusTest
@TestProfile(DevServicesCustomPortReusableServiceProfile.class)
public class DevServicesRedisCustomPortReusableServiceTest {

    @Test
    @DisplayName("should start redis container with the given custom port")
    public void shouldStartRedisContainer() {
        // We could strengthen this test to make sure the container is the same as seen by other tests, but it's hard since we won't know the order
        Assertions.assertTrue(SocketKit.isPortAlreadyUsed(6371));
    }

}
