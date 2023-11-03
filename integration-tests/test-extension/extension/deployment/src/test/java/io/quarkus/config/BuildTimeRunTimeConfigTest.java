package io.quarkus.config;

import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.ext.web.Router;

public class BuildTimeRunTimeConfigTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> {
                try {
                    String props = new String(FileUtil.readFileContents(
                            BuildTimeRunTimeConfigTest.class.getClassLoader().getResourceAsStream("application.properties")),
                            StandardCharsets.UTF_8);

                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(DevBean.class)
                            .addAsResource(new StringAsset(props + "\n" +
                                    "quarkus.application.name=my-app\n" +
                                    "quarkus.application.version=${quarkus.http.ssl.client-auth}"),
                                    "application.properties");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).setLogRecordPredicate(logRecord -> !logRecord.getMessage().contains("but it is build time fixed to"));

    @Test
    void buildTimeRunTimeConfig() {
        // A combination of QuarkusUnitTest and QuarkusProdModeTest tests ordering may mess with the port leaving it in
        // 8081 and QuarkusDevModeTest does not change to the right port.
        RestAssured.port = -1;

        RestAssured.when().get("/application").then()
                .statusCode(200)
                .body(is("my-app"));

        RestAssured.when().get("/tls").then()
                .statusCode(200)
                .body(is("false"));

        RestAssured.when().get("/source/quarkus.application.name").then()
                .statusCode(200)
                .body(is("BuildTime RunTime Fixed"));

        RestAssured.when().get("/source/quarkus.tls.trust-all").then()
                .statusCode(200)
                .body(is("BuildTime RunTime Fixed"));

        TEST.modifyResourceFile("application.properties", s -> s + "\n" +
                "quarkus.application.name=modified-app\n" +
                "quarkus.tls.trust-all=true\n");

        RestAssured.when().get("/application").then()
                .statusCode(200)
                .body(is("modified-app"));

        RestAssured.when().get("/tls").then()
                .statusCode(200)
                .body(is("true"));

        RestAssured.when().get("/source/quarkus.application.name").then()
                .statusCode(200)
                .body(is("BuildTime RunTime Fixed"));

        RestAssured.when().get("/source/quarkus.tls.trust-all").then()
                .statusCode(200)
                .body(is("BuildTime RunTime Fixed"));
    }

    @ApplicationScoped
    public static class DevBean {
        @Inject
        Router router;
        @Inject
        SmallRyeConfig config;
        @Inject
        ApplicationConfig applicationConfig;
        @Inject
        TlsConfig tlsConfig;

        public void register(@Observes StartupEvent ev) {
            router.get("/application").handler(rc -> rc.response().end(applicationConfig.name.get()));
            router.get("/tls").handler(rc -> rc.response().end(tlsConfig.trustAll + ""));
            router.get("/source/:name")
                    .handler(rc -> rc.response().end(config.getConfigValue(rc.pathParam("name")).getConfigSourceName()));
        }
    }
}
