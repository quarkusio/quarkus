package io.quarkus.resteasy.test;

import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.http.params.CoreConnectionPNames;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;

public class IncompletePostTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PostEndpoint.class));

    @TestHTTPResource
    URL url;

    @Test
    public void testIncompleteWrite() throws Exception {
        PostEndpoint.invoked = false;

        //make sure incomplete writes do not block threads
        //and that incoplete data is not delivered to the endpoint
        for (int i = 0; i < 1000; ++i) {
            Socket socket = new Socket(url.getHost(), url.getPort());
            socket.getOutputStream().write(
                    "POST /post HTTP/1.1\r\nHost: localhost\r\nContent-length:10\r\n\r\ntest".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            socket.getOutputStream().close();
            socket.close();
        }

        Assertions.assertFalse(PostEndpoint.invoked);
        RestAssuredConfig config = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000)
                        .setParam(CoreConnectionPNames.SO_TIMEOUT, 60000));

        RestAssured.given().config(config).get("/post").then().body(Matchers.is("ok"));
    }

}
