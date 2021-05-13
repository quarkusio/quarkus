package io.quarkus.redis.devservices.it;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.devservices.it.profiles.DevServicesDisabledProfile;
import io.quarkus.redis.devservices.it.utils.SocketKit;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DevServicesDisabledProfile.class)
public class DevServicesRedisDisabledITest {

    @Test
    @DisplayName("should not start the redis container when devservices is disabled")
    public void shouldStartRedisContainer() {
        Assertions.assertFalse(SocketKit.isPortAlreadyUsed(6379));
    }

}
