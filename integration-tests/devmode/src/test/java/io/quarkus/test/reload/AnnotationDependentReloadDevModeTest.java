package io.quarkus.test.reload;

import static org.hamcrest.Matchers.is;

import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

class AnnotationDependentReloadDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(AnnotationProcessorMarker.class)
                    .addClass(AddressData.class)
                    .addClass(ContactData.class)
                    .addClass(AddressMapper.class)
                    .addClass(AnnotationDependentReloadResource.class)
                    .addAsResource(new StringAsset(
                            """
                                    quarkus.dev.recompile-annotations=io.quarkus.test.reload.AnnotationProcessorMarker
                                    quarkus.grpc.dev-mode.force-server-start=false
                                    """),
                            "application.properties"))
            .setLogRecordPredicate(r -> true);

    // mostly meant as smoke test to ensure the recompile-annotations has any effect
    // more detailed testing is done as Unit Tests in AnnotationDependentClassesProcessorTest
    @Test
    void testDependencyChangeTriggersAnnotatedClassRecompilation() {
        RestAssured.get("/annotation-dependent-reload/test").then().body(is("hello"));

        // just a file change to make quarkus hot reload on next rest call
        TEST.modifySourceFile(ContactData.class, oldSource -> oldSource.replace(
                "}",
                "public String email;}"));

        RestAssured.get("/annotation-dependent-reload/test").then().body(is("hello"));

        // ContactData -> AddressMapper recompile
        // but not AdressData
        // since AdressData is not annotated
        boolean found = false;
        for (LogRecord logRecord : TEST.getLogRecords()) {
            if (logRecord.getLoggerName().equals("io.quarkus.deployment.dev.RuntimeUpdatesProcessor")
                    && (logRecord.getParameters()[0].equals("AddressMapper.class, ContactData.class")
                            || logRecord.getParameters()[0].equals("ContactData.class, AddressMapper.class"))) {
                found = true;
            }
        }
        Assertions.assertTrue(found, "Did not find a log record from RuntimeUpdatesProcessor for AddressMapper class");
    }
}
