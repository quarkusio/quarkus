package io.quarkus.it.flyway.mongodb;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.bson.Document;
import org.flywaydb.core.Flyway;

import com.mongodb.client.MongoClient;

import io.quarkus.flyway.mongodb.FlywayMongodbClient;
import io.quarkus.mongodb.MongoClientName;

@Path("/flyway-mongodb")
public class FlywayMongodbResource {

    @Inject
    MongoClient mongoClient;

    @Inject
    @MongoClientName("secondary")
    MongoClient secondaryClient;

    @Inject
    @MongoClientName("users")
    MongoClient usersClient;

    @Inject
    @MongoClientName("lazy")
    MongoClient lazyClient;

    @Inject
    @MongoClientName("custom-ph")
    MongoClient customPhClient;

    @Inject
    @FlywayMongodbClient("lazy")
    Flyway lazyFlyway;

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public long count() {
        return mongoClient.getDatabase("testdb").getCollection("fruits").countDocuments();
    }

    @GET
    @Path("/placeholder-color")
    @Produces(MediaType.TEXT_PLAIN)
    public String placeholderColor() {
        Document doc = mongoClient.getDatabase("testdb").getCollection("fruits")
                .find(new Document("name", "placeholder-fruit")).first();
        return doc != null ? doc.getString("color") : "not-found";
    }

    @GET
    @Path("/history-count")
    @Produces(MediaType.TEXT_PLAIN)
    public long historyCount() {
        return mongoClient.getDatabase("testdb").getCollection("flyway_schema_history").countDocuments();
    }

    @GET
    @Path("/secondary/count")
    @Produces(MediaType.TEXT_PLAIN)
    public long secondaryCount() {
        return secondaryClient.getDatabase("secondarydb").getCollection("vegetables").countDocuments();
    }

    @GET
    @Path("/users/count")
    @Produces(MediaType.TEXT_PLAIN)
    public long usersCount() {
        return usersClient.getDatabase("usersdb").getCollection("users").countDocuments();
    }

    @GET
    @Path("/users/history-count")
    @Produces(MediaType.TEXT_PLAIN)
    public long usersHistoryCount() {
        return usersClient.getDatabase("usersdb").getCollection("user_flyway_history").countDocuments();
    }

    @GET
    @Path("/users/index-exists")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean usersIndexExists() {
        Set<String> names = StreamSupport
                .stream(usersClient.getDatabase("usersdb").getCollection("users").listIndexes().spliterator(), false)
                .map(doc -> doc.getString("name"))
                .collect(Collectors.toSet());
        return names.contains("emailIdx");
    }

    @GET
    @Path("/lazy/count")
    @Produces(MediaType.TEXT_PLAIN)
    public long lazyCount() {
        return lazyClient.getDatabase("lazydb").getCollection("products").countDocuments();
    }

    @GET
    @Path("/callback-count")
    @Produces(MediaType.TEXT_PLAIN)
    public long callbackCount() {
        return mongoClient.getDatabase("testdb").getCollection("callback_log")
                .countDocuments(new Document("event", "beforeMigrate"));
    }

    @GET
    @Path("/before-each-migrate-callback-count")
    @Produces(MediaType.TEXT_PLAIN)
    public long beforeEachMigrateCallbackCount() {
        return mongoClient.getDatabase("testdb").getCollection("callback_log")
                .countDocuments(new Document("event", "beforeEachMigrate"));
    }

    @GET
    @Path("/after-each-migrate-callback-count")
    @Produces(MediaType.TEXT_PLAIN)
    public long afterEachMigrateCallbackCount() {
        return mongoClient.getDatabase("testdb").getCollection("callback_log")
                .countDocuments(new Document("event", "afterEachMigrate"));
    }

    @GET
    @Path("/after-migrate-callback-count")
    @Produces(MediaType.TEXT_PLAIN)
    public long afterMigrateCallbackCount() {
        return mongoClient.getDatabase("testdb").getCollection("callback_log")
                .countDocuments(new Document("event", "afterMigrate"));
    }

    @GET
    @Path("/lazy/migrate-and-count")
    @Produces(MediaType.TEXT_PLAIN)
    public long lazyMigrateAndCount() {
        lazyFlyway.migrate();
        return lazyClient.getDatabase("lazydb").getCollection("products").countDocuments();
    }

    @GET
    @Path("/custom-ph/color")
    @Produces(MediaType.TEXT_PLAIN)
    public String customPhColor() {
        Document doc = customPhClient.getDatabase("customphdb").getCollection("palette")
                .find(new Document("name", "primary")).first();
        return doc != null ? doc.getString("color") : "not-found";
    }
}
