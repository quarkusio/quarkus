package io.quarkus.mongo;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoWriteException;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.reactivestreams.client.MongoClients;

import io.quarkus.mongo.impl.ReactiveMongoClientImpl;

class ReactiveMongoClientTest extends MongoTestBase {

    private ReactiveMongoClient client;

    @BeforeEach
    void init() {
        client = new ReactiveMongoClientImpl(MongoClients.create(getConnectionString()));
    }

    @AfterEach
    void cleanup() {
        dropOurCollection(client);
        client.close();
    }

    @Test
    void testFindOneReturnsObjectWithId() {
        String collection = randomCollection();
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> myCollection = database.getCollection(collection);
        Document document = createDoc();
        myCollection.insertOne(document)
                .thenCompose(x -> myCollection.find(eq("foo", "bar")).findFirst().run())
                .thenAccept(opt -> {
                    Document found = opt.orElse(null);
                    assertThat(found).isNotNull();
                    assertThat(found.getObjectId("_id")).isNotNull();
                })
                .toCompletableFuture()
                .join();
    }

    @Test
    void testFindOneReturnsEmptyWhenNonMatches() {
        String collection = randomCollection();
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> myCollection = database.getCollection(collection);
        Document document = createDoc();
        myCollection.insertOne(document)
                .thenCompose(x -> myCollection.find(eq("nothing", "missing")).findFirst().run())
                .thenAccept(opt -> {
                    assertThat(opt).isEmpty();
                })
                .toCompletableFuture()
                .join();
    }

    @Test
    void testInsertPreexistingObjectID() {
        String collection = randomCollection();
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> myCollection = database.getCollection(collection);
        Document doc = createDoc();
        ObjectId value = new ObjectId();
        doc.put("_id", value);
        myCollection.insertOne(doc).toCompletableFuture().join();
        Optional<Document> optional = myCollection.find().findFirst().run().toCompletableFuture().join();
        assertThat(optional).isNotEmpty();
        assertThat(optional.orElse(new Document()).getObjectId("_id")).isEqualTo(value);
    }

    @Test
    void testInsertFollowedWithRetrieve() {
        String collection = randomCollection();
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> myCollection = database.getCollection(collection);
        Document doc = createDoc();
        ObjectId value = new ObjectId();
        doc.put("_id", value);
        myCollection.insertOne(doc).toCompletableFuture().join();
        Optional<Document> optional = myCollection.find().findFirst().run().toCompletableFuture().join();
        assertThat(optional).isNotEmpty();
        assertThat(optional.orElse(new Document()).getObjectId("_id")).isEqualTo(value);
        assertThat(optional.orElse(new Document())).isEqualTo(doc);
    }

    @Test
    void testInsertionFailedWhenDocumentExist() {
        String collection = randomCollection();
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> myCollection = database.getCollection(collection);
        Document doc = createDoc();
        ObjectId value = new ObjectId();
        doc.put("_id", value);
        myCollection.insertOne(doc).toCompletableFuture().join();
        try {
            myCollection.insertOne(doc).toCompletableFuture().join();
            fail("Write Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(CompletionException.class).hasCauseInstanceOf(MongoWriteException.class);
        }
    }

    @Test
    void testFindBatch() {
        String collectionName = randomCollection();
        ReactiveMongoCollection<Document> myCollection = client.getDatabase(DATABASE).getCollection(collectionName);

        List<Document> toBeInserted = new ArrayList<>();
        for (int i = 0; i < 3000; i++) {
            toBeInserted.add(createDoc(i));
        }
        List<Document> documents = new CopyOnWriteArrayList<>();
        myCollection.insertMany(toBeInserted)
                .thenCompose(x -> {
                    return ReactiveStreams.fromPublisher(myCollection.findAsPublisher().sort(eq("foo", 1)))
                            .forEach(documents::add)
                            .run();
                })
                .toCompletableFuture().join();

        assertThat(documents.size()).isEqualTo(3000);
        assertThat(documents.get(0).getString("foo")).isEqualTo("bar0");
        assertThat(documents.get(3000 - 1).getString("foo")).isEqualTo("bar999");
    }

    @Test
    void testFindBatchWithClass() {
        String collectionName = randomCollection();
        ReactiveMongoCollection<Document> myCollection = client.getDatabase(DATABASE).getCollection(collectionName);

        List<Document> toBeInserted = new ArrayList<>();
        for (int i = 0; i < 3000; i++) {
            toBeInserted.add(createDoc(i));
        }
        List<Document> documents = new CopyOnWriteArrayList<>();
        myCollection.insertMany(toBeInserted)
                .thenCompose(x -> {
                    return ReactiveStreams.fromPublisher(myCollection.findAsPublisher(Document.class).sort(eq("foo", 1)))
                            .forEach(documents::add)
                            .run();
                })
                .toCompletableFuture().join();

        assertThat(documents.size()).isEqualTo(3000);
        assertThat(documents.get(0).getString("foo")).isEqualTo("bar0");
        assertThat(documents.get(3000 - 1).getString("foo")).isEqualTo("bar999");
    }

    @Test
    void testFindBatchWithFilter() {
        String collectionName = randomCollection();
        ReactiveMongoCollection<Document> myCollection = client.getDatabase(DATABASE).getCollection(collectionName);

        List<Document> toBeInserted = new ArrayList<>();
        for (int i = 0; i < 3000; i++) {
            toBeInserted.add(createDoc(i));
        }
        List<Document> documents = new CopyOnWriteArrayList<>();
        myCollection.insertMany(toBeInserted)
                .thenCompose(x -> {
                    return ReactiveStreams.fromPublisher(myCollection.findAsPublisher(eq("num", 123))
                            .sort(eq("foo", 1)))
                            .forEach(documents::add)
                            .run();
                })
                .toCompletableFuture().join();

        assertThat(documents.size()).isEqualTo(3000);
        assertThat(documents.get(0).getString("foo")).isEqualTo("bar0");
        assertThat(documents.get(3000 - 1).getString("foo")).isEqualTo("bar999");
    }

    @Test
    void testFindBatchWithFilterAndClass() {
        String collectionName = randomCollection();
        ReactiveMongoCollection<Document> myCollection = client.getDatabase(DATABASE).getCollection(collectionName);

        List<Document> toBeInserted = new ArrayList<>();
        for (int i = 0; i < 3000; i++) {
            toBeInserted.add(createDoc(i));
        }
        List<Document> documents = new CopyOnWriteArrayList<>();
        myCollection.insertMany(toBeInserted)
                .thenCompose(x -> {
                    return ReactiveStreams.fromPublisher(myCollection.findAsPublisher(eq("num", 123), Document.class)
                            .sort(eq("foo", 1)))
                            .forEach(documents::add)
                            .run();
                })
                .toCompletableFuture().join();

        assertThat(documents.size()).isEqualTo(3000);
        assertThat(documents.get(0).getString("foo")).isEqualTo("bar0");
        assertThat(documents.get(3000 - 1).getString("foo")).isEqualTo("bar999");
    }

    @Test
    void testUpsertCreatesHexIfRecordDoesNotExist() {
        upsertDoc(randomCollection(), createDoc(), null).toCompletableFuture().join();
    }

    @Test
    void testUpsertWithASetOnInsertIsNotOverWritten() throws Exception {
        String collection = randomCollection();
        Document docToInsert = createDoc();
        Document insertStatement = new Document();
        insertStatement.put("$set", docToInsert);
        Document nested = new Document();
        nested.put("a-field", "an-entry");
        insertStatement.put("$setOnInsert", nested);

        upsertDoc(collection, docToInsert, insertStatement, null).thenAccept(saved -> {
            assertThat(saved).isNotEmpty();
            assertThat("an-entry").isEqualTo(saved.get().getString("a-field"));
        }).toCompletableFuture().join();

    }

    private CompletionStage<Optional<Document>> upsertDoc(String collection, Document docToInsert, String expectedId) {
        Document insertStatement = new Document();
        insertStatement.put("$set", docToInsert);
        return upsertDoc(collection, docToInsert, insertStatement, expectedId);
    }

    private CompletionStage<Optional<Document>> upsertDoc(String collection, Document docToInsert, Document insertStatement,
            String expectedId) {
        return client.getDatabase(DATABASE).getCollection(collection)
                .updateMany(eq("foo", docToInsert.getString("foo")),
                        insertStatement,
                        new UpdateOptions().upsert(true))
                .thenCompose(result -> {
                    assertThat(result.getModifiedCount()).isEqualTo(0);
                    if (expectedId == null) {
                        assertThat(0).isEqualTo(result.getMatchedCount());
                        assertThat(result.getUpsertedId()).isNotNull();
                    } else {
                        assertThat(1).isEqualTo(result.getMatchedCount());
                        assertThat(result.getUpsertedId()).isNull();
                    }

                    return client.getDatabase(DATABASE).getCollection(collection).find().findFirst().run();
                });
    }

    @Test
    void testAggregate() {
        final int numDocs = 200;

        final String collection = randomCollection();
        List<Document> pipeline = new ArrayList<>();
        Document doc1 = new Document();
        doc1.put("$regex", "bar1");
        Document doc2 = new Document();
        doc2.put("foo", doc1);
        Document doc3 = new Document();
        doc3.put("$match", doc2);
        pipeline.add(doc3);

        Document doc4 = new Document();
        doc4.put("$count", "foo_starting_with_bar1");
        pipeline.add(doc4);

        Optional<Integer> optional = client.getDatabase(DATABASE).createCollection(collection)
                .thenCompose(x -> insertDocs(client, collection, numDocs))
                .thenApply(x -> client.getDatabase(DATABASE).getCollection(collection).aggregate(pipeline))
                .toCompletableFuture().join()
                .findFirst()
                .run()
                .toCompletableFuture()
                .join()
                .map(doc -> doc.getInteger("foo_starting_with_bar1"));

        assertThat(optional).contains(111);
    }

}
