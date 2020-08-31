package io.quarkus.it.mongodb.panache.reactive;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
@Disabled("See https://github.com/quarkusio/quarkus/issues/11711")
class NativeMongodbPanacheResourceIT extends ReactiveMongodbPanacheResourceTest {

}
