package io.quarkus.hibernate.orm.panache.test.multiple_pu;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MultiplePUTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(EntityWithFirstPU.class, EntityWithSecondPU.class, PanacheTestResource.class)
                    .addAsManifestResource("META-INF/some-persistence.xml", "persistence.xml")
                    .addAsResource(new StringAsset(
                            "quarkus.datasource.url=jdbc:h2:mem:test\n" +
                                    "quarkus.datasource.driver=org.h2.Driver\n"),
                            "application.properties"));

    @Test
    public void panacheOperations() {
        /**
         * First entity operations
         */
        RestAssured.when().get("/persistence-unit/first/name-1").then().body(Matchers.is("1"));
        RestAssured.when().get("/persistence-unit/first/name-2").then().body(Matchers.is("2"));

        /**
         * second entity operations
         */
        RestAssured.when().get("/persistence-unit/second/name-1").then().body(Matchers.is("1"));
        RestAssured.when().get("/persistence-unit/second/name-2").then().body(Matchers.is("2"));
    }
}
