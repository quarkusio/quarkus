package io.quarkus.mongo;

import static com.mongodb.client.model.Filters.eq;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

public class DeploymentWatchTest extends MongoWithReplicasTestBase {

    Block<ChangeStreamDocument<Document>> printBlock = (s) -> System.out.println("CHANGE: " + s);

    //    @Test
    void test() throws InterruptedException {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27018");
        MongoDatabase database = mongoClient.getDatabase("test");
        MongoCollection<Document> collection = database.getCollection("restaurants");
        new Thread(() -> collection.watch().forEach(printBlock)).start();
        Document doc = new Document().append("hello", "world");
        collection.insertOne(doc);
        collection.updateOne(eq("hello", "world"), new Document("$set", new Document().append("hello", "foo")));
        new Thread(() -> collection.watch().forEach(printBlock)).start();
        Thread.sleep(5000);
    }
}
