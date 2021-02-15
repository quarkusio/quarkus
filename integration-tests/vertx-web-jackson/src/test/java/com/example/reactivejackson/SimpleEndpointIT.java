package com.example.reactivejackson;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
@TestHTTPEndpoint(SimpleEndpoint.class)
class SimpleEndpointIT extends SimpleEndpointTest {

}
