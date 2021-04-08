package io.quarkus.mongodb.reactive;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.inc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoNamespace;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClients;

import io.quarkus.mongodb.CollectionListOptions;
import io.quarkus.mongodb.DistinctOptions;
import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;

class CollectionManagementTest extends MongoTestBase {

    private ReactiveMongoClient client;

    @BeforeEach
    void init() {
        client = new ReactiveMongoClientImpl(MongoClients.create(getConnectionString()));
    }

    @AfterEach
    void cleanup() {
        client.getDatabase(DATABASE).drop().await().indefinitely();
        client.close();
    }

    @Test
    void testCollectionCreation() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        database.createCollection("cappedCollection",
                new CreateCollectionOptions().capped(true).sizeInBytes(0x100000)).await().indefinitely();
        assertThat(database.listCollectionNames()
                .collect().asList().await().indefinitely()).hasSize(1).containsExactly("cappedCollection");
        assertThat(database.listCollections().map(doc -> doc.getString("name"))
                .collect().asList().await().indefinitely()).hasSize(1).containsExactly("cappedCollection");
        assertThat(database.listCollections(Document.class).map(doc -> doc.getString("name"))
                .collect().asList().await().indefinitely()).hasSize(1).containsExactly("cappedCollection");

        assertThat(database.getCollection("cappedCollection").getNamespace().getDatabaseName()).isEqualTo(DATABASE);
        assertThat(database.getCollection("cappedCollection").getDocumentClass()).isEqualTo(Document.class);
    }

    @Test
    void testCollectionCreationWithOptions() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        database.createCollection("cappedCollection",
                new CreateCollectionOptions().capped(true).sizeInBytes(0x100000)).await().indefinitely();
        assertThat(database.listCollections().map(doc -> doc.getString("name"))
                .collect().asList().await().indefinitely()).hasSize(1).containsExactly("cappedCollection");
    }

    @Test
    void testCollectionDrop() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        database.createCollection("to-be-dropped").await().indefinitely();
        assertThat(database.listCollectionNames()
                .collect().asList().await().indefinitely()).hasSize(1).containsExactly("to-be-dropped");

        database.getCollection("to-be-dropped").drop().await().indefinitely();
        assertThat(database.listCollectionNames()
                .collect().asList().await().indefinitely()).hasSize(0);
    }

    @Test
    void testCollectionList() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        database.createCollection("test").await().indefinitely();

        assertThat(database.listCollectionNames().collect().asList().await().indefinitely()).contains("test");
        assertThat(database.listCollectionNames()
                .collect().asList().await().indefinitely()).contains("test");

        assertThat(database.listCollections().map(col -> col.getString("name"))
                .collect().asList().await().indefinitely()).contains("test");
        Assertions.assertThat(database.listCollections(new CollectionListOptions().filter(new Document("name", "test")))
                .map(col -> col.getString("name"))
                .collect().asList().await().indefinitely()).containsExactly("test");
        assertThat(database.listCollections(Document.class,
                new CollectionListOptions().filter(new Document("name", "test")))
                .map(col -> col.getString("name"))
                .collect().asList().await().indefinitely()).containsExactly("test");

        assertThat(database.listCollections()
                .map(doc -> doc.getString("name"))
                .collect().asList().await().indefinitely()).contains("test");
        assertThat(database.listCollections(Document.class)
                .map(doc -> doc.getString("name"))
                .collect().asList().await().indefinitely()).contains("test");
    }

    @Test
    void renameCollection() {
        String original = randomAlphaString(8);
        String newName = randomAlphaString(8);
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        database.createCollection(original).await().indefinitely();
        assertThat(database.listCollectionNames().collect().asList().await().indefinitely()).contains(original);
        ReactiveMongoCollection<Document> collection = database.getCollection(original);
        collection.renameCollection(new MongoNamespace(DATABASE, newName)).await().indefinitely();
        assertThat(database.listCollectionNames().collect().asList().await().indefinitely()).contains(newName)
                .doesNotContain(original);
    }

    @Test
    void aggregate() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes")
                                .append("stars", 5))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .subscribeAsCompletionStage(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain")
                                .append("stars", 1))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .subscribeAsCompletionStage())
                .join();

        List<Document> join = collection.aggregate(Arrays.asList(
                Aggregates.match(eq("type", "heroes")),
                Aggregates.group("$stars", sum("count", 1)))).collect().asList().await().indefinitely();
        assertThat(join).hasSize(2);

        join = collection.aggregate(Arrays.asList(
                Aggregates.match(eq("type", "heroes")),
                Aggregates.group("$stars", sum("count", 1))))
                .collect().asList().await().indefinitely();
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
        collection.insertMany(documents).await().indefinitely();

        // It contains the default index on _id.
        assertThat(collection.listIndexes().collect().asList().await().indefinitely()).hasSize(1);

        String i = collection.createIndex(new Document("i", 1), new IndexOptions().name("my-index"))
                .subscribeAsCompletionStage()
                .join();
        String j = collection.createIndex(new Document("foo", 1)).await().indefinitely();
        assertThat(i).isEqualTo("my-index");
        assertThat(j).isNotBlank();
        assertThat(collection.listIndexes().collect().asList().await().indefinitely()).hasSize(3);
        assertThat(
                collection.listIndexes().collect().asList().await().indefinitely())
                        .hasSize(3);

        collection.dropIndex(i).await().indefinitely();
        assertThat(collection.listIndexes().collect().asList().await().indefinitely()).hasSize(2);
        collection.dropIndexes().await().indefinitely();
        assertThat(collection.listIndexes().collect().asList().await().indefinitely()).hasSize(1);
        assertThat(collection.listIndexes(Document.class)
                .collect().asList()
                .await().indefinitely()).hasSize(1);
    }

    @Test
    void findAndUpdate() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes")
                                .append("stars", 5))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .subscribeAsCompletionStage(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain")
                                .append("stars", 1))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .subscribeAsCompletionStage())
                .join();

        Document frogman = collection.findOneAndUpdate(new Document("id", 3), inc("stars", 3),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)).await().indefinitely();
        Document batman = collection.findOneAndUpdate(new Document("id", 2), inc("stars", -1)).await().indefinitely();

        assertThat(frogman).contains(entry("stars", 4), entry("name", "frogman")); // Returned after update
        assertThat(batman).contains(entry("stars", 4), entry("name", "batman")); // Returned the before update

    }

    @Test
    void findAndReplace() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes")
                                .append("stars", 5))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .subscribeAsCompletionStage(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain")
                                .append("stars", 1))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .subscribeAsCompletionStage())
                .join();

        Document newVillain = new Document("id", 5).append("name", "lex lutor").append("type", "villain")
                .append("stars", 3);
        Document newHeroes = new Document("id", 6).append("name", "supergirl").append("type", "heroes")
                .append("stars", 2);

        Document frogman = collection.findOneAndReplace(new Document("id", 3), newVillain).await().indefinitely();
        Document supergirl = collection.findOneAndReplace(new Document("id", 2), newHeroes,
                new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER)).await().indefinitely();

        assertThat(frogman).contains(entry("stars", 1), entry("name", "frogman"));
        assertThat(supergirl).contains(entry("stars", 2), entry("name", "supergirl"));

    }

    @Test
    void findAndDelete() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes")
                                .append("stars", 5))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .subscribeAsCompletionStage(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain")
                                .append("stars", 1))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .subscribeAsCompletionStage())
                .join();

        Document frogman = collection.findOneAndDelete(new Document("id", 3)).await().indefinitely();
        Document superman = collection
                .findOneAndDelete(new Document("id", 1), new FindOneAndDeleteOptions().sort(new Document("id", 1)))
                .await().indefinitely();

        assertThat(frogman).contains(entry("stars", 1), entry("name", "frogman"));
        assertThat(superman).contains(entry("stars", 5), entry("name", "superman"));

    }

    @Test
    void updateOne() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes")
                                .append("stars", 5))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .subscribeAsCompletionStage(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain")
                                .append("stars", 1))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .subscribeAsCompletionStage())
                .join();

        UpdateResult result = collection
                .updateOne(new Document("id", 3), inc("stars", 3), new UpdateOptions().bypassDocumentValidation(true))
                .await().indefinitely();
        UpdateResult result2 = collection.updateOne(new Document("id", 2), inc("stars", -1)).await().indefinitely();
        UpdateResult result3 = collection.updateOne(new Document("id", 50), inc("stars", -1)).await().indefinitely();

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
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes")
                                .append("stars", 5))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .subscribeAsCompletionStage(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain")
                                .append("stars", 1))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .subscribeAsCompletionStage())
                .join();

        Document newVillain = new Document("id", 5).append("name", "lex lutor").append("type", "villain")
                .append("stars", 3);
        Document newHeroes = new Document("id", 6).append("name", "supergirl").append("type", "heroes")
                .append("stars", 2);

        UpdateResult result = collection
                .replaceOne(new Document("id", 3), newVillain, new ReplaceOptions().bypassDocumentValidation(true))
                .await().indefinitely();
        UpdateResult result2 = collection.replaceOne(new Document("id", 2), newHeroes).await().indefinitely();
        UpdateResult result3 = collection.replaceOne(new Document("id", 50), newHeroes).await().indefinitely();

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
                .await().indefinitely();

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
                new BulkWriteOptions().ordered(true)).await().indefinitely();

        assertThat(result.getDeletedCount()).isEqualTo(0);
        assertThat(result.getInsertedCount()).isEqualTo(3);

    }

    @Test
    void distinct() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes")
                                .append("stars", 5))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .subscribeAsCompletionStage(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain")
                                .append("stars", 1))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .subscribeAsCompletionStage())
                .join();

        List<String> list = collection.distinct("type", String.class).collect().asList().await().indefinitely();
        assertThat(list).containsExactlyInAnyOrder("heroes", "villain");
        list = collection.distinct("type", String.class).collect().asList()
                .await().indefinitely();
        assertThat(list).containsExactlyInAnyOrder("heroes", "villain");

        list = collection.distinct("name", String.class, new DistinctOptions().filter(eq("name", "superman")))
                .collect().asList()
                .await().indefinitely();
        assertThat(list).hasSize(1);
        list = collection.distinct("name", String.class).collect().asList()
                .await().indefinitely();
        assertThat(list).hasSize(4);

        list = collection.distinct("name", eq("type", "villain"), String.class).collect().asList().await()
                .indefinitely();
        assertThat(list).hasSize(2);
    }

    @Test
    void find() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection("test");

        CompletableFuture.allOf(
                collection
                        .insertOne(new Document("id", 1).append("name", "superman").append("type", "heroes")
                                .append("stars", 5))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 2).append("name", "batman").append("type", "heroes").append("stars", 4))
                        .subscribeAsCompletionStage(),
                collection
                        .insertOne(new Document("id", 3).append("name", "frogman").append("type", "villain")
                                .append("stars", 1))
                        .subscribeAsCompletionStage(),
                collection.insertOne(
                        new Document("id", 4).append("name", "joker").append("type", "villain").append("stars", 5))
                        .subscribeAsCompletionStage())
                .join();

        assertThat(collection.find().collect().asList().await().indefinitely()).hasSize(4);
        Assertions
                .assertThat(collection.find(new FindOptions().comment("hello")).collect().asList().await().indefinitely())
                .hasSize(4);
        assertThat(collection.find().collect().asList().await().indefinitely())
                .hasSize(4);

        assertThat(collection.find(Document.class).collect().asList().await().indefinitely()).hasSize(4);
        assertThat(collection.find(Document.class, new FindOptions().skip(1)).collect().asList().await()
                .indefinitely())
                        .hasSize(3);
        assertThat(collection.find(Document.class).collect().asList()
                .await().indefinitely()).hasSize(4);

        assertThat(collection.find(eq("type", "heroes")).collect().asList().await().indefinitely()).hasSize(2);
        assertThat(
                collection.find(eq("type", "heroes"), new FindOptions()).collect().asList().await().indefinitely())
                        .hasSize(2);
        assertThat(collection.find(eq("type", "heroes")).collect().asList()
                .await().indefinitely()).hasSize(2);

        assertThat(collection.find(eq("type", "heroes"), Document.class).collect().asList().await().indefinitely())
                .hasSize(2);
        assertThat(collection.find(eq("type", "heroes"), Document.class, new FindOptions().partial(true)).collect()
                .asList()
                .await().indefinitely()).hasSize(2);
        assertThat(collection.find(eq("type", "heroes"), Document.class).collect().asList()
                .await().indefinitely()).hasSize(2);
    }

}
