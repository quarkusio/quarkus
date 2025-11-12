package io.quarkus.test.reload;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.commons.classloading.ClassLoaderHelper;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

class RecompilationDependenciesDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(AddressData.class)
                    .addClass(ContactData.class)
                    .addClass(AddressMapper.class)
                    .addClass(RecompilationDependenciesResource.class)
                    // load with TCCL so that arc can load this extension during build time
                    .addClass("io.quarkus.test.reload.RecompilationDependenciesBuildCompatibleExtension")
                    .addAsResource(new StringAsset("io.quarkus.test.reload.RecompilationDependenciesBuildCompatibleExtension"),
                            "META-INF/services/jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension")
                    .addAsResource(new StringAsset(
                            """
                                    quarkus.grpc.dev-mode.force-server-start=false
                                    """),
                            "application.properties"))
            .setLogRecordPredicate(r -> true);

    // mostly meant as smoke test to ensure the recompile-dependencies has any effect
    // more testing is done as Unit Tests in RecompilationDependenciesProcessorTest
    // See also RecompilationDependenciesBuildCompatibleExtension, which acts as source of recompilation dependency tree
    // Since no extension is around to produce an actual RecompilationDependenciesBuildItem
    @Test
    void testDependencyChangeTriggersRecompilationOfRecompilationTargets() {
        Map<String, Long> originalFileTimes = RestAssured.given().accept(ContentType.JSON)
                .get("/recompile-dependencies/test").then().extract().body()
                .as(Map.class);

        // just a file change to make quarkus hot reload on next rest call
        TEST.modifySourceFile(ContactData.class, oldSource -> oldSource.replace(
                "}",
                "public String email;}"));

        // ContactData -> AddressMapper recompile
        // but not AddressData
        // since not present in builditems

        // First check that both files have been recompiled
        RestAssured.given().accept(ContentType.JSON).get("/recompile-dependencies/test").then()
                .body(AddressData.class.getSimpleName(), is(originalFileTimes.get(AddressData.class.getSimpleName())))
                .body(ContactData.class.getSimpleName(), greaterThan(originalFileTimes.get(ContactData.class.getSimpleName())))
                .body(AddressMapper.class.getSimpleName(),
                        greaterThan(originalFileTimes.get(AddressMapper.class.getSimpleName())));

        // and just to be safe, check that this is also presented to user
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

    @ApplicationScoped
    @Path("/recompile-dependencies")
    public static class RecompilationDependenciesResource {

        @GET
        @Path("/test")
        @Produces(MediaType.WILDCARD)
        public String test() throws URISyntaxException, IOException {

            return """
                    {
                        "%s": %s,
                        "%s": %s,
                        "%s": %s
                    }
                    """.formatted(//
                    AddressData.class.getSimpleName(), fileTime(AddressData.class), //
                    ContactData.class.getSimpleName(), fileTime(ContactData.class), //
                    AddressMapper.class.getSimpleName(), fileTime(AddressMapper.class)//
            );
        }

        private long fileTime(Class<?> clazz) throws URISyntaxException, IOException {
            return Files.getLastModifiedTime(pathToClass(clazz))
                    .toMillis();
        }

        private static java.nio.file.Path pathToClass(Class<?> clazz) throws URISyntaxException {
            return java.nio.file.Path
                    .of(clazz.getClassLoader().getResource(ClassLoaderHelper.fromClassNameToResourceName(clazz.getName()))
                            .toURI());
        }
    }
}
