package io.quarkus.mongodb;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.inc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bson.Document;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoNamespace;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClients;

import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;

class CollectionManagementTest extends MongoTestBase {

    private ReactiveMongoClient client;

    @BeforeEach
    void init() {
        client = new ReactiveMongoClientImpl(MongoClients.create(getConnectionString()));
    }

    @AfterEach
    void cleanup() {
        client.getDatabase(DATABASE).drop().toCompletableFuture().join();
        client.close();
    }

    @Test
    void testCollectionCreation() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        database.createCollection("cappedCollection",
                new CreateCollectionOptions().capped(true).sizeInBytes(0x100000)).toCompletableFuture().join();
        assertThat(database.listCollectionNames()
                .toList().run().toCompletableFuture().join()).hasSize(1).containsExactly("cappedCollection");
        assertThat(database.listCollections().map(doc -> doc.getString("name"))
                .toList().run().toCompletableFuture().join()).hasSize(1).containsExactly("cappedCollection");
        assertThat(database.listCollections(Document.class).map(doc -> doc.getString("name"))
                .toList().run().toCompletableFuture().join()).hasSize(1).containsExactly("cappedCollection");

        assertThat(database.getCollection("cappedCollection").getNamespace().getDatabaseName()).isEqualTo(DATABASE);
        assertThat(database.getCollection("cappedCollection").getDocumentClass()).isEqualTo(Document.class);
    }

    @Test
    void testCollectionCreationWithOptions() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        database.createCollection("cappedCollection",
                new CreateCollectionOptions().capped(true).sizeInBytes(0x100000)).toCompletableFuture().join();
        assertThat(database.listCollections().map(doc -> doc.getString("name"))
                .toList().run().toCompletableFuture().join()).hasSize(1).containsExactly("cappedCollection");
    }

    @Test
    void testCollectionDrop() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        database.createCollection("to-be-dropped").toCompletableFuture().join();
        assertThat(database.listCollectionNames()
                .toList().run().toCompletableFuture().join()).hasSize(1).containsExactly("to-be-dropped");

        database.getCollection("to-be-dropped").drop().toCompletableFuture().join();
        assertThat(database.listCollectionNames()
                .toList().run().toCompletableFuture().join()).hasSize(0);
    }

    @Test
    void testCollectionList() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        database.createCollection("test").toCompletableFuture().join();

        assertThat(database.listCollectionNames().toList().run().toCompletableFuture().join()).contains("test");
        assertThat(ReactiveStreams.fromPublisher(database.listCollectionNamesAsPublisher())
                .toList().run().toCompletableFuture().join()).contains("test");

        assertThat(database.listCollections().map(col -> col.getString("name"))
                .toList().run().toCompletableFuture().join()).contains("test");
        assertThat(database.listCollections(new CollectionListOptions().filter(new Document("name", "test")))
                .map(col -> col.getString("name"))
                .toList().run().toCompletableFuture().join()).containsExactly("test");
        assertThat(database.listCollections(Document.class, new CollectionListOptions().filter(new Document("name", "test")))
                .map(col -> col.getString("name"))
                .toList().run().toCompletableFuture().join()).containsExactly("test");

        assertThat(ReactiveStreams.fromPublisher(database.listCollectionsAsPublisher())
                .map(doc -> doc.getString("name"))
                .toList().run().toCompletableFuture().join()).contains("test");
        assertThat(ReactiveStreams.fromPublisher(database.listCollectionsAsPublisher(Document.class))
                .map(doc -> doc.getString("name"))
                .toList().run().toCompletableFuture().join()).contains("test");
    }

    @Test
    void renameCollection() {
        String original = randomAlphaString(8);
        String newName = randomAlphaString(8);
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        database.createCollection(original).toCompletableFuture().join();
        assertThat(database.listCollectionNames().toList().run().toCompletableFuture().join()).contains(original);
        ReactiveMongoCollection<Document> collection = database.getCollection(original);
        collection.renameCollection(new MongoNamespace(DATABASE, newName)).toCompletableFuture().join();
        assertThat(database.listCollectionNames().toList().run().toCompletableFuture().join()).contains(newName)
                .doesNotContain(original);
    }

    @Test
    void aggregate() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes").append("stars", 5))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .toCompletableFuture(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain").append("stars", 1))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .toCompletableFuture())
                .join();

        List<Document> join = collection.aggregate(Arrays.asList(
                Aggregates.match(eq("type", "heroes")),
                Aggregates.group("$stars", sum("count", 1)))).toList().run().toCompletableFuture().join();
        assertThat(join).hasSize(2);

        join = ReactiveStreams.fromPublisher(collection.aggregateAsPublisher(Arrays.asList(
                Aggregates.match(eq("type", "heroes")),
                Aggregates.group("$stars", sum("count", 1))))).toList().run().toCompletableFuture().join();
        assertThat(join).hasSize(2);
    }

    @Test
    void indexes() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection(randomAlphaString(8));

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i).append("foo", "bar" + i));
        }
        collection.insertMany(documents).toCompletableFuture().join();

        // It contains the default index on _id.
        assertThat(collection.listIndexes().toList().run().toCompletableFuture().join()).hasSize(1);

        String i = collection.createIndex(new Document("i", 1), new IndexOptions().name("my-index")).toCompletableFuture()
                .join();
        String j = collection.createIndex(new Document("foo", 1)).toCompletableFuture().join();
        assertThat(i).isEqualTo("my-index");
        assertThat(j).isNotBlank();
        assertThat(collection.listIndexes().toList().run().toCompletableFuture().join()).hasSize(3);
        assertThat(
                ReactiveStreams.fromPublisher(collection.listIndexesAsPublisher()).toList().run().toCompletableFuture().join())
                        .hasSize(3);

        collection.dropIndex(i).toCompletableFuture().join();
        assertThat(collection.listIndexes().toList().run().toCompletableFuture().join()).hasSize(2);
        collection.dropIndexes().toCompletableFuture().join();
        assertThat(collection.listIndexes().toList().run().toCompletableFuture().join()).hasSize(1);
        assertThat(ReactiveStreams.fromPublisher(collection.listIndexesAsPublisher(Document.class)).toList().run()
                .toCompletableFuture().join()).hasSize(1);
    }

    @Test
    void findAndUpdate() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes").append("stars", 5))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .toCompletableFuture(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain").append("stars", 1))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .toCompletableFuture())
                .join();

        Document frogman = collection.findOneAndUpdate(new Document("id", 3), inc("stars", 3),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)).toCompletableFuture().join();
        Document batman = collection.findOneAndUpdate(new Document("id", 2), inc("stars", -1)).toCompletableFuture().join();

        assertThat(frogman).contains(entry("stars", 4), entry("name", "frogman")); // Returned after update
        assertThat(batman).contains(entry("stars", 4), entry("name", "batman")); // Returned the before update

    }

    @Test
    void findAndReplace() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes").append("stars", 5))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .toCompletableFuture(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain").append("stars", 1))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .toCompletableFuture())
                .join();

        Document newVillain = new Document("id", 5).append("name", "lex lutor").append("type", "villain").append("stars", 3);
        Document newHeroes = new Document("id", 6).append("name", "supergirl").append("type", "heroes").append("stars", 2);

        Document frogman = collection.findOneAndReplace(new Document("id", 3), newVillain).toCompletableFuture().join();
        Document supergirl = collection.findOneAndReplace(new Document("id", 2), newHeroes,
                new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER)).toCompletableFuture().join();

        assertThat(frogman).contains(entry("stars", 1), entry("name", "frogman"));
        assertThat(supergirl).contains(entry("stars", 2), entry("name", "supergirl"));

    }

    @Test
    void findAndDelete() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes").append("stars", 5))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .toCompletableFuture(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain").append("stars", 1))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .toCompletableFuture())
                .join();

        Document frogman = collection.findOneAndDelete(new Document("id", 3)).toCompletableFuture().join();
        Document superman = collection
                .findOneAndDelete(new Document("id", 1), new FindOneAndDeleteOptions().sort(new Document("id", 1)))
                .toCompletableFuture().join();

        assertThat(frogman).contains(entry("stars", 1), entry("name", "frogman"));
        assertThat(superman).contains(entry("stars", 5), entry("name", "superman"));

    }

    @Test
    void updateOne() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes").append("stars", 5))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .toCompletableFuture(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain").append("stars", 1))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .toCompletableFuture())
                .join();

        UpdateResult result = collection
                .updateOne(new Document("id", 3), inc("stars", 3), new UpdateOptions().bypassDocumentValidation(true))
                .toCompletableFuture().join();
        UpdateResult result2 = collection.updateOne(new Document("id", 2), inc("stars", -1)).toCompletableFuture().join();
        UpdateResult result3 = collection.updateOne(new Document("id", 50), inc("stars", -1)).toCompletableFuture().join();

        assertThat(result.getMatchedCount()).isEqualTo(1);
        assertThat(result.getModifiedCount()).isEqualTo(1);
        assertThat(result2.getMatchedCount()).isEqualTo(1);
        assertThat(result2.getModifiedCount()).isEqualTo(1);
        assertThat(result3.getMatchedCount()).isEqualTo(0);
        assertThat(result3.getModifiedCount()).isEqualTo(0);

    }

    @Test
    void replaceOne() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes").append("stars", 5))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .toCompletableFuture(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain").append("stars", 1))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .toCompletableFuture())
                .join();

        Document newVillain = new Document("id", 5).append("name", "lex lutor").append("type", "villain").append("stars", 3);
        Document newHeroes = new Document("id", 6).append("name", "supergirl").append("type", "heroes").append("stars", 2);

        UpdateResult result = collection
                .replaceOne(new Document("id", 3), newVillain, new ReplaceOptions().bypassDocumentValidation(true))
                .toCompletableFuture().join();
        UpdateResult result2 = collection.replaceOne(new Document("id", 2), newHeroes).toCompletableFuture().join();
        UpdateResult result3 = collection.replaceOne(new Document("id", 50), newHeroes).toCompletableFuture().join();

        assertThat(result.getMatchedCount()).isEqualTo(1);
        assertThat(result.getModifiedCount()).isEqualTo(1);
        assertThat(result2.getMatchedCount()).isEqualTo(1);
        assertThat(result2.getModifiedCount()).isEqualTo(1);
        assertThat(result3.getMatchedCount()).isEqualTo(0);
        assertThat(result3.getModifiedCount()).isEqualTo(0);
    }

    @Test
    void bulkWrite() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        BulkWriteResult result = collection.bulkWrite(Arrays.asList(
                new InsertOneModel<>(new Document("_id", 4)),
                new InsertOneModel<>(new Document("_id", 5)),
                new InsertOneModel<>(new Document("_id", 6)),
                new UpdateOneModel<>(new Document("_id", 1),
                        new Document("$set", new Document("x", 2))),
                new DeleteOneModel<>(new Document("_id", 2)),
                new ReplaceOneModel<>(new Document("_id", 3),
                        new Document("_id", 3).append("x", 4))))
                .toCompletableFuture().join();

        assertThat(result.getDeletedCount()).isEqualTo(0);
        assertThat(result.getInsertedCount()).isEqualTo(3);

    }

    @Test
    void bulkWriteWithOptions() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        BulkWriteResult result = collection.bulkWrite(Arrays.asList(
                new InsertOneModel<>(new Document("_id", 4)),
                new InsertOneModel<>(new Document("_id", 5)),
                new InsertOneModel<>(new Document("_id", 6)),
                new UpdateOneModel<>(new Document("_id", 1),
                        new Document("$set", new Document("x", 2))),
                new DeleteOneModel<>(new Document("_id", 2)),
                new ReplaceOneModel<>(new Document("_id", 3),
                        new Document("_id", 3).append("x", 4))),
                new BulkWriteOptions().ordered(true)).toCompletableFuture().join();

        assertThat(result.getDeletedCount()).isEqualTo(0);
        assertThat(result.getInsertedCount()).isEqualTo(3);

    }

    @Test
    void distinct() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes").append("stars", 5))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .toCompletableFuture(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain").append("stars", 1))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .toCompletableFuture())
                .join();

        List<String> list = collection.distinct("type", String.class).toList().run().toCompletableFuture().join();
        assertThat(list).containsExactlyInAnyOrder("heroes", "villain");
        list = ReactiveStreams.fromPublisher(collection.distinctAsPublisher("type", String.class)).toList().run()
                .toCompletableFuture().join();
        assertThat(list).containsExactlyInAnyOrder("heroes", "villain");

        list = collection.distinct("name", String.class, new DistinctOptions().filter(eq("name", "superman"))).toList().run()
                .toCompletableFuture().join();
        assertThat(list).hasSize(1);
        list = ReactiveStreams.fromPublisher(collection.distinctAsPublisher("name", String.class)).toList().run()
                .toCompletableFuture().join();
        assertThat(list).hasSize(4);

        list = collection.distinct("name", eq("type", "villain"), String.class).toList().run().toCompletableFuture().join();
        assertThat(list).hasSize(2);
    }

    @Test
    void find() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes").append("stars", 5))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .toCompletableFuture(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain").append("stars", 1))
                        .toCompletableFuture(),
                collection.insertOne(new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .toCompletableFuture())
                .join();

        assertThat(collection.find().toList().run().toCompletableFuture().join()).hasSize(4);
        assertThat(collection.find(new FindOptions().comment("hello")).toList().run().toCompletableFuture().join()).hasSize(4);
        assertThat(ReactiveStreams.fromPublisher(collection.findAsPublisher()).toList().run().toCompletableFuture().join())
                .hasSize(4);

        assertThat(collection.find(Document.class).toList().run().toCompletableFuture().join()).hasSize(4);
        assertThat(collection.find(Document.class, new FindOptions().skip(1)).toList().run().toCompletableFuture().join())
                .hasSize(3);
        assertThat(ReactiveStreams.fromPublisher(collection.findAsPublisher(Document.class)).toList().run()
                .toCompletableFuture().join()).hasSize(4);

        assertThat(collection.find(eq("type", "heroes")).toList().run().toCompletableFuture().join()).hasSize(2);
        assertThat(collection.find(eq("type", "heroes"), new FindOptions()).toList().run().toCompletableFuture().join())
                .hasSize(2);
        assertThat(ReactiveStreams.fromPublisher(collection.findAsPublisher(eq("type", "heroes"))).toList().run()
                .toCompletableFuture().join()).hasSize(2);

        assertThat(collection.find(eq("type", "heroes"), Document.class).toList().run().toCompletableFuture().join())
                .hasSize(2);
        assertThat(collection.find(eq("type", "heroes"), Document.class, new FindOptions().partial(true)).toList().run()
                .toCompletableFuture().join()).hasSize(2);
        assertThat(ReactiveStreams.fromPublisher(collection.findAsPublisher(eq("type", "heroes"), Document.class)).toList()
                .run().toCompletableFuture().join()).hasSize(2);

    }

}
