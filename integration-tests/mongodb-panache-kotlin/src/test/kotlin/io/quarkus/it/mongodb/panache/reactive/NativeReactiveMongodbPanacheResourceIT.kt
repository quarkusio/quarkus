package io.quarkus.it.mongodb.panache.reactive

import io.quarkus.test.junit.NativeImageTest
import org.junit.jupiter.api.Disabled

@NativeImageTest
@Disabled("See https://github.com/quarkusio/quarkus/issues/11711")
internal class NativeReactiveMongodbPanacheResourceIT : ReactiveMongodbPanacheResourceTest()
