package io.quarkus.devservices.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ContainerAddressTest {

    @Test
    public void getUrlWithIpv6Host() {
        assertThat(new ContainerAddress("id", "fd00:d0ca:1::1", 27017).getUrl())
                .isEqualTo("[fd00:d0ca:1::1]:27017");
    }

    @Test
    public void getUrlWithAlias() {
        assertThat(new ContainerAddress("id", "redis-abc12", 6379).getUrl()).isEqualTo("redis-abc12:6379");
    }
}
