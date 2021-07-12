package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.config.SmallRyeConfig;

@QuarkusTest
public class AppConfigMockTest {
    @Inject
    AppConfig appConfig;

    @Test
    void mockAppConfig() {
        given()
                .get("/app-config/name")
                .then()
                .statusCode(OK.getStatusCode())
                .body(is("app"));

        Mockito.when(appConfig.name()).thenReturn("mocked-app");

        given()
                .get("/app-config/name")
                .then()
                .statusCode(OK.getStatusCode())
                .body(is("mocked-app"));

        given()
                .get("/app-config/info/alias")
                .then()
                .statusCode(OK.getStatusCode())
                .body(is("alias"));

        Mockito.when(appConfig.info().alias()).thenReturn("mocked-alias");

        given()
                .get("/app-config/info/alias")
                .then()
                .statusCode(OK.getStatusCode())
                .body(is("mocked-alias"));
    }

    public static class AppConfigProducer {
        @Inject
        Config config;

        @Produces
        @ApplicationScoped
        @Mock
        AppConfig appConfig() {
            AppConfig appConfig = config.unwrap(SmallRyeConfig.class).getConfigMapping(AppConfig.class);
            AppConfig appConfigSpy = Mockito.spy(appConfig);
            AppConfig.Info infoSpy = Mockito.spy(appConfig.info());
            Mockito.when(appConfigSpy.info()).thenReturn(infoSpy);
            return appConfigSpy;
        }
    }
}
