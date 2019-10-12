package io.quarkus.mongodb;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.inc;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClients;

import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;

class BasicInteractionTest extends MongoTestBase {

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
    void testConnection() {
        assertThat(client.listDatabases().findFirst().run().toCompletableFuture().join()).isNotEmpty();
    }

    @Test
    void testRetrievingADatabaseAndDropIt() {
        String name = randomAlphaString(8);
        String col = randomAlphaString(8);
        ReactiveMongoDatabase database = client.getDatabase(name);
        assertThat(database).isNotNull();
        database.createCollection(col).toCompletableFuture().join();
        assertThat(database.listCollectionNames().findFirst().run().toCompletableFuture().join()).contains(col);
        database.drop().toCompletableFuture().join();
        assertThat(client.listDatabaseNames().toList().run().toCompletableFuture().join()).doesNotContain(name);
    }

    @Test
    void testDocumentInsertion() {
        List<Integer> books = Arrays.asList(27464, 747854);
        DBObject person = new BasicDBObject("_id", "jo")
                .append("name", "Jo Bloggs")
                .append("address", new BasicDBObject("street", "123 Fake St")
                        .append("city", "Faketon")
                        .append("state", "MA")
                        .append("zip", 12345))
                .append("books", books);

        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<DBObject> collection = database.getCollection(randomAlphaString(8), DBObject.class);
        collection.insertOne(person).toCompletableFuture().join();

        Optional<DBObject> maybe = collection.find().findFirst().run().toCompletableFuture().join();
        assertThat(maybe).isNotEmpty().containsInstanceOf(DBObject.class)
                .hasValueSatisfying(obj -> assertThat(obj.get("name")).isEqualTo("Jo Bloggs"));
    }

    @Test
    void testDocumentInsertionWithOptions() {
        List<Integer> books = Arrays.asList(27464, 747854);
        DBObject person = new BasicDBObject("_id", "jo")
                .append("name", "Jo Bloggs")
                .append("address", new BasicDBObject("street", "123 Fake St")
                        .append("city", "Faketon")
                        .append("state", "MA")
                        .append("zip", 12345))
                .append("books", books);

        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<DBObject> collection = database.getCollection(randomAlphaString(8), DBObject.class);
        collection.insertOne(person, new InsertOneOptions().bypassDocumentValidation(true)).toCompletableFuture().join();

        Optional<DBObject> maybe = collection.find().findFirst().run().toCompletableFuture().join();
        assertThat(maybe).isNotEmpty().containsInstanceOf(DBObject.class)
                .hasValueSatisfying(obj -> assertThat(obj.get("name")).isEqualTo("Jo Bloggs"));
    }

    @Test
    void testInsertionOfManyDocumentsAndQueries() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection(randomAlphaString(8));

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }

        collection.insertMany(documents).toCompletableFuture().join();
        Long count = collection.countDocuments().toCompletableFuture().join();
        Long countWithOption = collection.countDocuments(new Document(), new CountOptions().limit(10)).toCompletableFuture()
                .join();
        Long estimated = collection.estimatedDocumentCount().toCompletableFuture().join();
        Long estimatedWithOptions = collection
                .estimatedDocumentCount(new EstimatedDocumentCountOptions().maxTime(10, TimeUnit.SECONDS)).toCompletableFuture()
                .join();
        assertThat(count).isEqualTo(100);
        assertThat(countWithOption).isEqualTo(10);
        assertThat(estimated).isEqualTo(100);
        assertThat(estimatedWithOptions).isEqualTo(100);

        count = collection.countDocuments(eq("i", 10)).toCompletableFuture().join();
        assertThat(count).isEqualTo(1);

        Optional<Document> document = collection.find().findFirst().run().toCompletableFuture().join();
        assertThat(document).isNotEmpty().hasValueSatisfying(doc -> assertThat(doc.get("i", 0)));

        document = collection.find(eq("i", 20)).findFirst().run().toCompletableFuture().join();
        assertThat(document).isNotEmpty().hasValueSatisfying(doc -> assertThat(doc.get("i", 20)));

        List<Document> list = collection.find().toList().run().toCompletableFuture().join();
        assertThat(list).hasSize(100);

        list = collection.find(gt("i", 50)).toList().run().toCompletableFuture().join();
        assertThat(list).hasSize(49);
    }

    @Test
    void testInsertionWithOptionsOfManyDocumentsAndQueries() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection(randomAlphaString(8));

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }

        collection.insertMany(documents, new InsertManyOptions().ordered(true)).toCompletableFuture().join();
        Long count = collection.countDocuments().toCompletableFuture().join();
        assertThat(count).isEqualTo(100);

        count = collection.countDocuments(eq("i", 10)).toCompletableFuture().join();
        assertThat(count).isEqualTo(1);

        Optional<Document> document = collection.find().findFirst().run().toCompletableFuture().join();
        assertThat(document).isNotEmpty().hasValueSatisfying(doc -> assertThat(doc.get("i", 0)));

        document = collection.find(eq("i", 20)).findFirst().run().toCompletableFuture().join();
        assertThat(document).isNotEmpty().hasValueSatisfying(doc -> assertThat(doc.get("i", 20)));

        List<Document> list = collection.find().toList().run().toCompletableFuture().join();
        assertThat(list).hasSize(100);

        list = collection.find(gt("i", 50)).toList().run().toCompletableFuture().join();
        assertThat(list).hasSize(49);
    }

    @Test
    void testSingleDocumentUpdate() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection(randomAlphaString(8));

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }

        collection.insertMany(documents).toCompletableFuture().join();
        Long count = collection.countDocuments().toCompletableFuture().join();
        assertThat(count).isEqualTo(100);

        UpdateResult result = collection.updateOne(eq("i", 10), new Document("$set", new Document("i", 110)))
                .toCompletableFuture().join();
        assertThat(result.getModifiedCount()).isEqualTo(1);
        assertThat(result.getMatchedCount()).isEqualTo(1);

        assertThat(collection.find(eq("i", 10)).findFirst().run().toCompletableFuture().join()).isEmpty();
        assertThat(collection.find(eq("i", 110)).findFirst().run().toCompletableFuture().join()).isNotEmpty();

    }

    @Test
    void testMultiDocumentUpdate() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection(randomAlphaString(8));

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }

        collection.insertMany(documents).toCompletableFuture().join();
        Long count = collection.countDocuments().toCompletableFuture().join();
        assertThat(count).isEqualTo(100);

        UpdateResult result = collection.updateMany(lt("i", 100), inc("i", 100)).toCompletableFuture().join();
        assertThat(result.getModifiedCount()).isEqualTo(100);
        assertThat(result.getMatchedCount()).isEqualTo(100);

        assertThat(collection.find(eq("i", 10)).findFirst().run().toCompletableFuture().join()).isEmpty();
        assertThat(collection.find(eq("i", 20)).findFirst().run().toCompletableFuture().join()).isEmpty();
        assertThat(collection.find(eq("i", 110)).findFirst().run().toCompletableFuture().join()).isNotEmpty();
        assertThat(collection.find(eq("i", 120)).findFirst().run().toCompletableFuture().join()).isNotEmpty();
    }

    @Test
    void testSingleDocumentDeletion() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection(randomAlphaString(8));

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }
        collection.insertMany(documents).toCompletableFuture().join();

        DeleteResult result = collection.deleteOne(eq("i", 10)).toCompletableFuture().join();
        assertThat(result.getDeletedCount()).isEqualTo(1);
        assertThat(collection.find(eq("i", 10)).findFirst().run().toCompletableFuture().join()).isEmpty();
        Long count = collection.countDocuments().toCompletableFuture().join();
        assertThat(count).isEqualTo(99);
    }

    @Test
    void testSingleDocumentDeletionWithOptions() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection(randomAlphaString(8));

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }
        collection.insertMany(documents).toCompletableFuture().join();

        DeleteResult result = collection.deleteOne(eq("i", 10),
                new DeleteOptions().collation(
                        Collation.builder().locale("en").caseLevel(true).build()))
                .toCompletableFuture().join();
        assertThat(result.getDeletedCount()).isEqualTo(1);
        assertThat(collection.find(eq("i", 10)).findFirst().run().toCompletableFuture().join()).isEmpty();
        Long count = collection.countDocuments().toCompletableFuture().join();
        assertThat(count).isEqualTo(99);
    }

    @Test
    void testMultipleDocumentDeletion() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection(randomAlphaString(8));

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }
        collection.insertMany(documents).toCompletableFuture().join();

        DeleteResult result = collection.deleteMany(gte("i", 90)).toCompletableFuture().join();
        assertThat(result.getDeletedCount()).isEqualTo(10);
        assertThat(collection.find(eq("i", 90)).findFirst().run().toCompletableFuture().join()).isEmpty();
        Long count = collection.countDocuments().toCompletableFuture().join();
        assertThat(count).isEqualTo(90);
    }

    @Test
    void testMultipleDocumentDeletionWithOptions() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection(randomAlphaString(8));

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }
        collection.insertMany(documents).toCompletableFuture().join();

        DeleteResult result = collection.deleteMany(gte("i", 90), new DeleteOptions().collation(
                Collation.builder().locale("en").caseLevel(true).build()))
                .toCompletableFuture().join();
        assertThat(result.getDeletedCount()).isEqualTo(10);
        assertThat(collection.find(eq("i", 90)).findFirst().run().toCompletableFuture().join()).isEmpty();
        Long count = collection.countDocuments().toCompletableFuture().join();
        assertThat(count).isEqualTo(90);
    }

    @Test
    void testIndexCreation() {
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
        assertThat(i).isEqualTo("my-index");
        assertThat(collection.listIndexes().toList().run().toCompletableFuture().join()).hasSize(2);

        collection.dropIndex(i).toCompletableFuture().join();
        assertThat(collection.listIndexes().toList().run().toCompletableFuture().join()).hasSize(1);

    }

}
