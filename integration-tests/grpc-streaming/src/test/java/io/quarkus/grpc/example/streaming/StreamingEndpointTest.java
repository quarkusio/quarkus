package io.quarkus.grpc.example.streaming;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

@QuarkusTest
class StreamingEndpointTest {

    protected static final TypeRef<List<String>> LIST_OF_STRING = new TypeRef<List<String>>() {
    };

    @Test
    public void testSource() {
        List<String> response = get("/streaming").as(LIST_OF_STRING);
        assertThat(response).containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    public void testPipe() {
        Response r = get("/streaming/3");
        List<String> response = r.as(LIST_OF_STRING);
        assertThat(response).containsExactly("0", "0", "1", "3");
    }

    @Test
    public void testSink() {
        get("/streaming/sink/3")
                .then().statusCode(204);
    }

}
