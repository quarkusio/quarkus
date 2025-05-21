package io.quarkus.redis.devservices.it;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.devservices.it.profiles.DevServicesCustomPortReusableServiceProfile;
import io.quarkus.redis.devservices.it.utils.SocketKit;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@Disabled("Tracked by https://github.com/quarkusio/quarkus/issues/45785")
@QuarkusTest
@TestProfile(DevServicesCustomPortReusableServiceProfile.class)
public class DevServicesRedisCustomPortReusableServiceITest {

    @Test
    @DisplayName("should start redis container with the given custom port")
    public void shouldStartRedisContainer() {
        Assertions.assertTrue(SocketKit.isPortAlreadyUsed(6371));
    }

}
