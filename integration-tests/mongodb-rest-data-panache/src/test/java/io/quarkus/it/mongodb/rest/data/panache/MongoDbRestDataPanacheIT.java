package io.quarkus.it.mongodb.rest.data.panache;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
@DisabledOnOs(OS.WINDOWS)
class MongoDbRestDataPanacheIT extends MongoDbRestDataPanacheTest {
}
