package io.quarkus.flyway.mongodb.test;

import static org.hamcrest.Matchers.is;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbDevModeAddMigrationTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DocCountEndpoint.class)
                    .addAsResource(new StringAsset("db.createCollection('migtest');"),
                            "db/migration/V1__init.js")
                    .addAsResource(new StringAsset(
                            "quarkus.mongodb.connection-string=" + FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING + "\n"
                                    + "quarkus.mongodb.database=devmodeadd\n"
                                    + "quarkus.flyway-mongodb.migrate-at-start=true\n"
                                    + "quarkus.flyway-mongodb.clean-at-start=true\n"
                                    + "quarkus.flyway-mongodb.database=devmodeadd"),
                            "application.properties"));

    @Test
    public void testAddingMigrationScriptCausesRestart() {
        RestAssured.get("/doc-count").then().statusCode(200).body(is("0"));
        config.addResourceFile("db/migration/V2__add_doc.js",
                "db.migtest.insertOne({name:'test'});");
        RestAssured.get("/doc-count").then().statusCode(200).body(is("1"));
    }

    @Path("/doc-count")
    public static class DocCountEndpoint {

        @Inject
        MongoClient mongoClient;

        @GET
        public long docCount() {
            return mongoClient.getDatabase("devmodeadd").getCollection("migtest").countDocuments();
        }
    }
}
