package io.quarkus.vertx.http.devmode.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@Disabled("See https://github.com/quarkusio/quarkus/pull/48171")
public class LiveReloadManagementTest {

    private static final String APP_PROPS = """
            quarkus.class-loading.reloadable-artifacts=io.vertx:vertx-web-client
            quarkus.management.enabled=true
            """;

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(LiveReloadManagementEndpoint.class));

    private final String managementPath = "http://localhost:9000";

    @Test
    public void test() {
        // 1. We start with a single management endpoint /manage-1
        String firstClassToString = RestAssured.get(managementPath + "/manage-1")
                .then()
                .statusCode(200)
                .extract().body().asString();
        // 2. We change the path of that endpoint, esentailly testing adding new and removing old one:
        test.modifySourceFile(LiveReloadManagementEndpoint.class, s -> s.replace("\"/manage-1\"", "\"/manage-2\""));

        // 2.1 Wait for the reload to be done, and first make sure that the "new" endpoint is there:
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String secondClassToString = RestAssured.get(managementPath + "/manage-2")
                            .then()
                            .statusCode(200)
                            .extract().body().asString();
                    assertThat(firstClassToString).isNotEqualTo(secondClassToString);
                });

        // 2.2 Since the above check was a success, it means that the app was reloaded and we are sure that
        // the /manage-1 is no longer available:
        RestAssured.get(managementPath + "/manage-1")
                .then()
                .statusCode(404);

        // 3. Now we want to add another endpoint that attempts to access a CDI bean
        // 4. First let's make sure that it is not there already:
        RestAssured.get(managementPath + "/manage-cdi").then().statusCode(404);

        // 5. Add/update all necessary source files:
        test.addSourceFile(LiveReloadManagementBean.class);
        test.addSourceFile(LiveReloadManagementHandler.class);
        test.modifySourceFile(LiveReloadManagementEndpoint.class, s -> s.replace("// Case 2: ", ""));

        // 6. Now wait for the app to get reloaded:
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String response = RestAssured.get(managementPath + "/manage-cdi")
                            .then()
                            .statusCode(200)
                            .extract().body().asString();
                    assertThat(response).isEqualTo("string1");
                });

        // 7. Update the bean used in the handler:
        test.modifySourceFile(LiveReloadManagementBean.class, s -> s.replace("string1", "string2"));

        // 8. Wait for the app to get reloaded and management endpoint should be using the reloaded CDI bean:
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String response = RestAssured.get(managementPath + "/manage-cdi")
                            .then()
                            .statusCode(200)
                            .extract().body().asString();
                    assertThat(response).isEqualTo("string2");
                });

        // 9. Now let's add yet another handler, this time the handler is injected as a bean. (case 3)
        test.addSourceFile(LiveReloadManagementHandlerAsCDIBean.class);
        test.modifySourceFile(LiveReloadManagementEndpoint.class, s -> s.replace("// Case 3: ", ""));

        // 10. Now wait for the app to get reloaded:
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String response = RestAssured.get(managementPath + "/manage-bean-handler")
                            .then()
                            .statusCode(200)
                            .extract().body().asString();
                    assertThat(response).isEqualTo("I'm a CDI bean handler.");
                });
    }

}
