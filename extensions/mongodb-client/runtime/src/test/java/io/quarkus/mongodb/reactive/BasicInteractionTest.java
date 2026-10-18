package io.quarkus.mongodb.reactive;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Updates.inc;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.EstimatedDocumentCountOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
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
        client.getDatabase(DATABASE).drop().await().indefinitely();
        client.close();
    }

    @Test
    void testConnection() {
        assertThat(client.listDatabases()
                .collect().first()
                .await().indefinitely()).isNotNull();
    }

    @Test
    void testRetrievingADatabaseAndDropIt() {
        String name = randomAlphaString(8);
        String col = randomAlphaString(8);
        ReactiveMongoDatabase database = client.getDatabase(name);
        assertThat(database).isNotNull();
        database.createCollection(col).await().indefinitely();
        assertThat(database.listCollectionNames().collect().first().await().indefinitely()).contains(col);
        database.drop().await().indefinitely();
        assertThat(client.listDatabaseNames().collect().asList().await().indefinitely()).doesNotContain(name);
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
        collection.insertOne(person).await().indefinitely();

        Optional<DBObject> maybe = collection.find().collect().first().await().asOptional().indefinitely();
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
        collection.insertOne(person, new InsertOneOptions().bypassDocumentValidation(true)).await().indefinitely();

        Optional<DBObject> maybe = collection.find().collect().first().await().asOptional().indefinitely();
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

        collection.insertMany(documents).await().indefinitely();
        Long count = collection.countDocuments().await().indefinitely();
        Long countWithOption = collection.countDocuments(new Document(), new CountOptions().limit(10))
                .await().indefinitely();
        Long estimated = collection.estimatedDocumentCount().await().indefinitely();
        Long estimatedWithOptions = collection
                .estimatedDocumentCount(new EstimatedDocumentCountOptions().maxTime(10, TimeUnit.SECONDS))
                .await().indefinitely();
        assertThat(count).isEqualTo(100);
        assertThat(countWithOption).isEqualTo(10);
        assertThat(estimated).isEqualTo(100);
        assertThat(estimatedWithOptions).isEqualTo(100);

        count = collection.countDocuments(eq("i", 10)).await().indefinitely();
        assertThat(count).isEqualTo(1);

        Optional<Document> document = collection.find().collect().first().await().asOptional().indefinitely();
        assertThat(document).isNotEmpty().hasValueSatisfying(doc -> assertThat(doc.get("i", 0)));

        document = collection.find(eq("i", 20)).collect().first().await().asOptional().indefinitely();
        assertThat(document).isNotEmpty().hasValueSatisfying(doc -> assertThat(doc.get("i", 20)));

        List<Document> list = collection.find().collect().asList().await().indefinitely();
        assertThat(list).hasSize(100);

        list = collection.find(gt("i", 50)).collect().asList().await().indefinitely();
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

        collection.insertMany(documents, new InsertManyOptions().ordered(true)).await().indefinitely();
        Long count = collection.countDocuments().await().indefinitely();
        assertThat(count).isEqualTo(100);

        count = collection.countDocuments(eq("i", 10)).await().indefinitely();
        assertThat(count).isEqualTo(1);

        Optional<Document> document = collection.find().collect().first().await().asOptional().indefinitely();
        assertThat(document).isNotEmpty().hasValueSatisfying(doc -> assertThat(doc.get("i", 0)));

        document = collection.find(eq("i", 20)).collect().first().await().asOptional().indefinitely();
        assertThat(document).isNotEmpty().hasValueSatisfying(doc -> assertThat(doc.get("i", 20)));

        List<Document> list = collection.find().collect().asList().await().indefinitely();
        assertThat(list).hasSize(100);

        list = collection.find(gt("i", 50)).collect().asList().await().indefinitely();
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

        collection.insertMany(documents).await().indefinitely();
        Long count = collection.countDocuments().await().indefinitely();
        assertThat(count).isEqualTo(100);

        UpdateResult result = collection.updateOne(eq("i", 10), new Document("$set", new Document("i", 110)))
                .await().indefinitely();
        assertThat(result.getModifiedCount()).isEqualTo(1);
        assertThat(result.getMatchedCount()).isEqualTo(1);

        assertThat(collection.find(eq("i", 10)).collect().first().await().asOptional().indefinitely()).isEmpty();
        assertThat(collection.find(eq("i", 110)).collect().first().await().asOptional().indefinitely()).isNotEmpty();

    }

    @Test
    void testMultiDocumentUpdate() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection(randomAlphaString(8));

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }

        collection.insertMany(documents).await().indefinitely();
        Long count = collection.countDocuments().await().indefinitely();
        assertThat(count).isEqualTo(100);

        UpdateResult result = collection.updateMany(lt("i", 100), inc("i", 100)).await().indefinitely();
        assertThat(result.getModifiedCount()).isEqualTo(100);
        assertThat(result.getMatchedCount()).isEqualTo(100);

        assertThat(collection.find(eq("i", 10)).collect().first().await().indefinitely()).isNull();
        assertThat(collection.find(eq("i", 20)).collect().first().await().indefinitely()).isNull();
        assertThat(collection.find(eq("i", 110)).collect().first().await().indefinitely()).isNotNull();
        assertThat(collection.find(eq("i", 120)).collect().first().await().indefinitely()).isNotNull();
    }

    @Test
    void testSingleDocumentDeletion() {
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> collection = database.getCollection(randomAlphaString(8));

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(new Document("i", i));
        }
        collection.insertMany(documents).await().indefinitely();

        DeleteResult result = collection.deleteOne(eq("i", 10)).await().indefinitely();
        assertThat(result.getDeletedCount()).isEqualTo(1);
        assertThat(collection.find(eq("i", 10)).collect().first().await().indefinitely()).isNull();
        Long count = collection.countDocuments().await().indefinitely();
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
        collection.insertMany(documents).await().indefinitely();

        DeleteResult result = collection.deleteOne(eq("i", 10),
                new DeleteOptions().collation(
                        Collation.builder().locale("en").caseLevel(true).build()))
                .await().indefinitely();
        assertThat(result.getDeletedCount()).isEqualTo(1);
        assertThat(collection.find(eq("i", 10)).collect().first().await().indefinitely()).isNull();
        Long count = collection.countDocuments().await().indefinitely();
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
        collection.insertMany(documents).await().indefinitely();

        DeleteResult result = collection.deleteMany(gte("i", 90)).await().indefinitely();
        assertThat(result.getDeletedCount()).isEqualTo(10);
        assertThat(collection.find(eq("i", 90)).collect().first().await().indefinitely()).isNull();
        Long count = collection.countDocuments().await().indefinitely();
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
        collection.insertMany(documents).await().indefinitely();

        DeleteResult result = collection.deleteMany(gte("i", 90), new DeleteOptions().collation(
                Collation.builder().locale("en").caseLevel(true).build()))
                .await().indefinitely();
        assertThat(result.getDeletedCount()).isEqualTo(10);
        assertThat(collection.find(eq("i", 90)).collect().first().await().asOptional().indefinitely()).isEmpty();
        Long count = collection.countDocuments().await().indefinitely();
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
        collection.insertMany(documents).await().indefinitely();

        // It contains the default index on _id.
        assertThat(collection.listIndexes().collect().asList().await().indefinitely()).hasSize(1);

        String i = collection.createIndex(new Document("i", 1), new IndexOptions().name("my-index"))
                .await().indefinitely();
        assertThat(i).isEqualTo("my-index");
        assertThat(collection.listIndexes().collect().asList().await().indefinitely()).hasSize(2);

        collection.dropIndex(i).await().indefinitely();
        assertThat(collection.listIndexes().collect().asList().await().indefinitely()).hasSize(1);

    }

}
