package io.quarkus.it.reactive.pg.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.util.List;
import java.util.function.Function;
import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class HotReloadTestCase {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HotReloadFruitResource.class)
                    .addAsResource("application-tl.properties", "application.properties"))
            .setLogRecordPredicate(record -> {
                return record.getLoggerName().startsWith("io.quarkus.reactive.datasource");
            });

    @AfterAll
    public static void afterAll() {
        List<LogRecord> records = TEST.getLogRecords();
        Assertions.assertEquals(8, records.size());
        // make sure that we closed all thread-local pools on reload and close
        Assertions.assertEquals("Making pool for thread: %s", records.get(0).getMessage());
        Assertions.assertEquals("Making pool for thread: %s", records.get(1).getMessage());
        Assertions.assertEquals("Closing pool: %s", records.get(2).getMessage());
        Assertions.assertEquals("Closing pool: %s", records.get(3).getMessage());
        Assertions.assertEquals("Making pool for thread: %s", records.get(4).getMessage());
        Assertions.assertEquals("Making pool for thread: %s", records.get(5).getMessage());
        Assertions.assertEquals("Closing pool: %s", records.get(6).getMessage());
        Assertions.assertEquals("Closing pool: %s", records.get(7).getMessage());
    }

    @Test
    public void testAddNewFieldToEntity() {
        checkRequest("Orange");
        TEST.modifySourceFile(HotReloadFruitResource.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("'Orange'", "'Strawberry'");
            }
        });
        // trigger a pool hot reload by changing the config
        TEST.modifyResourceFile("application.properties", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("quarkus.datasource.reactive.thread-local=true",
                        "quarkus.datasource.reactive.thread-local = true");
            }
        });

        checkRequest("Strawberry");
    }

    private void checkRequest(String fruit) {
        given()
                .when().get("/hot-fruits")
                .then()
                .statusCode(200)
                .body(
                        containsString(fruit),
                        containsString("Pear"),
                        containsString("Apple"));
    }
}
