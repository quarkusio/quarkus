package io.quarkus.reactive.datasource;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

public abstract class ChangingCredentialsTestBase {

    private String user1;
    private String user2;

    protected ChangingCredentialsTestBase(String user1, String user2) {
        this.user1 = user1;
        this.user2 = user2;
    }

    @Test
    public void testConnect() throws Exception {
        given().when().get("/test").then().statusCode(200).body(CoreMatchers.equalTo(user1));

        SECONDS.sleep(2); // sleep longer than pool idle connection timeout

        given().when().get("/test").then().statusCode(200).body(CoreMatchers.equalTo(user2));
    }

}
