package io.quarkus.mongodb.reactive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.mongodb.client.result.InsertOneResult;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.runtime.Network;
import io.smallrye.mutiny.Uni;

public class MongoTestBase {

    private static final Logger LOGGER = Logger.getLogger(MongoTestBase.class);
    public static final String COLLECTION_PREFIX = "mongo-extension-test-";
    public static final String DATABASE = "mongo-extension-test-db";
    private static MongodExecutable MONGO;

    protected static String getConfiguredConnectionString() {
        return getProperty("connection_string");
    }

    protected static String getProperty(String name) {
        String s = System.getProperty(name);
        if (s != null) {
            s = s.trim();
            if (s.length() > 0) {
                return s;
            }
        }

        return null;
    }

    @BeforeAll
    public static void startMongoDatabase() throws IOException {
        String uri = getConfiguredConnectionString();
        // This switch allow testing against a running mongo database.
        if (uri == null) {
            Version.Main version = Version.Main.V4_0;
            int port = 27018;
            LOGGER.infof("Starting Mongo %s on port %s", version, port);
            IMongodConfig config = new MongodConfigBuilder()
                    .version(version)
                    .net(new Net(port, Network.localhostIsIPv6()))
                    .build();
            MONGO = getMongodExecutable(config);
            try {
                MONGO.start();
            } catch (Exception e) {
                //every so often mongo fails to start on CI runs
                //see if this helps
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
                MONGO.start();
            }
        } else {
            LOGGER.infof("Using existing Mongo %s", uri);
        }
    }

    private static MongodExecutable getMongodExecutable(IMongodConfig config) {
        try {
            return doGetExecutable(config);
        } catch (Exception e) {
            // sometimes the download process can timeout so just sleep and try again
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
            return doGetExecutable(config);
        }
    }

    private static MongodExecutable doGetExecutable(IMongodConfig config) {
        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                .defaults(Command.MongoD)
                .processOutput(ProcessOutput.getDefaultInstanceSilent())
                .build();
        return MongodStarter.getInstance(runtimeConfig).prepare(config);
    }

    @AfterAll
    public static void stopMongoDatabase() {
        if (MONGO != null) {
            try {
                MONGO.stop();
            } catch (Exception e) {
                LOGGER.error("Unable to stop MongoDB", e);
            }
        }
    }

    protected String getConnectionString() {
        if (getConfiguredConnectionString() != null) {
            return getConfiguredConnectionString();
        } else {
            return "mongodb://localhost:27018";
        }
    }

    public static String randomAlphaString(int length) {
        StringBuilder builder = new StringBuilder(length);

        for (int i = 0; i < length; ++i) {
            char c = (char) ((int) (65.0D + 25.0D * Math.random()));
            builder.append(c);
        }

        return builder.toString();
    }

    protected List<io.quarkus.mongodb.reactive.ReactiveMongoCollection<Document>> getOurCollections(
            io.quarkus.mongodb.reactive.ReactiveMongoClient client) {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        List<String> names = database.listCollectionNames().collectItems().asList().await().indefinitely();
        return names
                .stream()
                .filter(c -> c.startsWith(COLLECTION_PREFIX))
                .map(database::getCollection)
                .collect(Collectors.toList());
    }

    protected void dropOurCollection(io.quarkus.mongodb.reactive.ReactiveMongoClient client) {
        List<io.quarkus.mongodb.reactive.ReactiveMongoCollection<Document>> collections = getOurCollections(client);
        for (ReactiveMongoCollection<Document> col : collections) {
            col.drop().await().indefinitely();
        }
    }

    protected String randomCollection() {
        return COLLECTION_PREFIX + randomAlphaString(20);
    }

    protected Uni<Void> insertDocs(ReactiveMongoClient mongoClient, String collection, int num) {
        io.quarkus.mongodb.reactive.ReactiveMongoDatabase database = mongoClient.getDatabase(DATABASE);
        io.quarkus.mongodb.reactive.ReactiveMongoCollection<Document> mongoCollection = database
                .getCollection(collection);
        List<CompletableFuture<InsertOneResult>> list = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            Document doc = createDoc(i);
            list.add(mongoCollection.insertOne(doc).subscribeAsCompletionStage());
        }
        CompletableFuture<InsertOneResult>[] array = list.toArray(new CompletableFuture[0]);
        return Uni.createFrom().completionStage(CompletableFuture.allOf(array));
    }

    protected Document createDoc() {
        Document document = new Document();
        document.put("foo", "bar");
        document.put("num", 123);
        document.put("big", true);
        document.put("nullentry", null);

        Document nested = new Document();
        nested.put("wib", "wob");
        document.put("arr", Arrays.asList("x", true, 1.23, null, nested));
        document.put("date", new Date());
        Document other = new Document();
        other.put("quux", "flib");
        other.put("myarr", Arrays.asList("blah", true, 312));
        document.put("other", other);
        return document;
    }

    protected Document createDoc(int num) {
        Document document = new Document();
        document.put("foo", "bar" + (num != -1 ? num : ""));
        document.put("num", 123);
        document.put("big", true);
        document.put("nullentry", null);

        Document nested = new Document();
        nested.put("wib", "wob");
        document.put("arr", Arrays.asList("x", true, 12, 1.23, null, nested));
        document.put("date", new Date());
        document.put("object_id", new ObjectId());
        Document other = new Document();
        other.put("quux", "flib");
        other.put("myarr", Arrays.asList("blah", true, 312));
        document.put("other", other);
        document.put("longval", 123456789L);
        document.put("dblval", 1.23);
        return document;
    }
}
