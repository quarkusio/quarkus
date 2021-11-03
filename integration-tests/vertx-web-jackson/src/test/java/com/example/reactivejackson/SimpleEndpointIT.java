package com.example.reactivejackson;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@TestHTTPEndpoint(SimpleEndpoint.class)
class SimpleEndpointIT extends SimpleEndpointTest {

}
