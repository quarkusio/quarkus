package io.quarkus.redis.it.lettuce;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class LettuceBackendIT extends LettuceBackendTest {

    @Override
    String getKey(String k) {
        return "native-" + k;
    }
}
