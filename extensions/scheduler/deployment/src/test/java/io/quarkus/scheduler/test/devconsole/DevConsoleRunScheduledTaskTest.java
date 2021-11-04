package io.quarkus.scheduler.test.devconsole;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DevConsoleRunScheduledTaskTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(NeverRunTask.class));

    @Test
    public void testInvokeScheduledTask() {
        RestAssured.with()
                .get("empty")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("true"));
        RestAssured.with().formParam("name", "io.quarkus.scheduler.test.devconsole.NeverRunTask#run")
                .redirects().follow(false)
                .post("q/dev/io.quarkus.quarkus-scheduler/schedules")
                .then()
                .statusCode(303);
        RestAssured.with()
                .get("status")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("task ran"));
    }

}
