package io.quarkus.smallrye.reactivemessaging.devmode;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.reactivemessaging.config.DumbConnector;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.response.Response;

public class ConfigurationLiveReloadTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyProcessor.class, HttpFrontend.class, DumbConnector.class)
                    .addAsResource(
                            new File("src/test/resources/config/dummy-connector-conf.properties"),
                            "application.properties"));

    @Test
    public void testConfigurationUpdate() {
        String s = get("http://localhost:8080").asString();
        assertThat(s).isEqualTo("HALLOHALLO");

        test.modifyResourceFile("application.properties", v -> v.replace("hallo", "up"));

        s = get("http://localhost:8080").asString();
        assertThat(s).isEqualTo("UPUP");
    }

    @Test
    public void testConfigurationRemovalAndRecovery() {
        String s = get("http://localhost:8080").asString();
        assertThat(s).isEqualTo("HALLOHALLO");

        test.modifyResourceFile("application.properties", v -> v.replace("mp.messaging.incoming.input.values=hallo", "#$$"));

        Response response = get("http://localhost:8080").andReturn();
        assertThat(response.getStatusCode()).isEqualTo(500);

        test.modifyResourceFile("application.properties", v -> v.replace("#$$", "mp.messaging.incoming.input.values=foo"));
        s = get("http://localhost:8080").asString();
        assertThat(s).isEqualTo("FOOFOO");
    }

    @Test
    public void testProcessorCodeChange() {
        String s = get("http://localhost:8080").asString();
        assertThat(s).isEqualTo("HALLOHALLO");

        test.modifySourceFile("MyProcessor.java", d -> d.replace("input.toUpperCase()", "input.toUpperCase() + \"!\""));

        s = get("http://localhost:8080").asString();
        assertThat(s).isEqualTo("HALLO!HALLO!");
    }

    @Test
    public void testSubscriberCodeChange() {
        String s = get("http://localhost:8080").asString();
        assertThat(s).isEqualTo("HALLOHALLO");

        test.modifySourceFile("HttpFrontend.java", d -> d.replace("response.end();", "response.write(\"!\"); response.end();"));

        s = get("http://localhost:8080").asString();
        assertThat(s).isEqualTo("HALLOHALLO!");
    }

}
