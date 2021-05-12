package io.quarkus.mongodb.reactive;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoWriteException;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.reactivestreams.client.MongoClients;

import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.impl.ReactiveMongoClientImpl;
import io.smallrye.mutiny.Uni;

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
                .chain(() -> myCollection.find(eq("foo", "bar")).collect().first())
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getObjectId("_id")).isNotNull();
                })
                .await().indefinitely();
    }

    @Test
    void testFindOneReturnsEmptyWhenNonMatches() {
        String collection = randomCollection();
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> myCollection = database.getCollection(collection);
        Document document = createDoc();
        myCollection.insertOne(document)
                .chain(() -> myCollection.find(eq("nothing", "missing")).collect().first())
                .invoke(opt -> assertThat(opt).isNull())
                .await().indefinitely();
    }

    @Test
    void testInsertPreexistingObjectID() {
        String collection = randomCollection();
        ReactiveMongoDatabase database = client.getDatabase(DATABASE);
        ReactiveMongoCollection<Document> myCollection = database.getCollection(collection);
        Document doc = createDoc();
        ObjectId value = new ObjectId();
        doc.put("_id", value);
        myCollection.insertOne(doc).await().indefinitely();
        Optional<Document> optional = myCollection.find().collect().first().await().asOptional().indefinitely();
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
        myCollection.insertOne(doc).await().indefinitely();
        Optional<Document> optional = myCollection.find().collect().first().await().asOptional().indefinitely();
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
        myCollection.insertOne(doc).await().indefinitely();
        try {
            myCollection.insertOne(doc).await().indefinitely();
            fail("Write Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(MongoWriteException.class);
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
                .chain(() -> myCollection.find(new FindOptions().sort(eq("foo", 1)))
                        .onItem().invoke(documents::add)
                        .onItem().ignoreAsUni())
                .await().indefinitely();

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
                .chain(() -> myCollection.find(Document.class, new FindOptions().sort(eq("foo", 1)))
                        .onItem().invoke(documents::add)
                        .onItem().ignoreAsUni())
                .await().indefinitely();

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
                .chain(() -> myCollection.find(new FindOptions().filter(eq("num", 123)).sort(eq("foo", 1)))
                        .onItem().invoke(documents::add)
                        .onItem().ignoreAsUni())
                .await().indefinitely();

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
                .chain(() -> myCollection.find(Document.class,
                        new FindOptions().filter(eq("num", 123)).sort(eq("foo", 1)))
                        .onItem().invoke(documents::add)
                        .onItem().ignoreAsUni())
                .await().indefinitely();

        assertThat(documents.size()).isEqualTo(3000);
        assertThat(documents.get(0).getString("foo")).isEqualTo("bar0");
        assertThat(documents.get(3000 - 1).getString("foo")).isEqualTo("bar999");
    }

    @Test
    void testUpsertCreatesHexIfRecordDoesNotExist() {
        upsertDoc(randomCollection(), createDoc(), null).await().indefinitely();
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

        upsertDoc(collection, docToInsert, insertStatement, null).onItem().invoke(saved -> {
            assertThat(saved).isNotNull();
            assertThat("an-entry").isEqualTo(saved.getString("a-field"));
        }).await().indefinitely();

    }

    private Uni<Document> upsertDoc(String collection, Document docToInsert, String expectedId) {
        Document insertStatement = new Document();
        insertStatement.put("$set", docToInsert);
        return upsertDoc(collection, docToInsert, insertStatement, expectedId);
    }

    private Uni<Document> upsertDoc(String collection, Document docToInsert,
            Document insertStatement,
            String expectedId) {
        return client.getDatabase(DATABASE).getCollection(collection)
                .updateMany(eq("foo", docToInsert.getString("foo")),
                        insertStatement,
                        new UpdateOptions().upsert(true))
                .chain(result -> {
                    assertThat(result.getModifiedCount()).isEqualTo(0);
                    if (expectedId == null) {
                        assertThat(0).isEqualTo(result.getMatchedCount());
                        assertThat(result.getUpsertedId()).isNotNull();
                    } else {
                        assertThat(1).isEqualTo(result.getMatchedCount());
                        assertThat(result.getUpsertedId()).isNull();
                    }

                    return client.getDatabase(DATABASE).getCollection(collection).find().collect().first();
                });
    }

    @Test
    void testAggregate() {
        final int numDocs = 100;

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
                .chain(() -> insertDocs(client, collection, numDocs))
                .onItem().transformToMulti(x -> client.getDatabase(DATABASE).getCollection(collection).aggregate(pipeline))
                .collect().first()
                .onItem().transform(doc -> doc.getInteger("foo_starting_with_bar1"))
                .await().asOptional().indefinitely();
        assertThat(optional).contains(11);
    }

}
