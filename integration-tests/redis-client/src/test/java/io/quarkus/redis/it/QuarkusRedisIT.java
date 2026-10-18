package io.quarkus.redis.it;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class QuarkusRedisIT extends QuarkusRedisTest {

    @Override
    String getKey(String k) {
        return "native-" + k;
    }
}
