package io.quarkus.it.mongodb;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

@Path("/connectionStatus")
public class ConnectionStatusResource {

    @Inject
    MongoClient client;

    private MongoDatabase database;

    @PostConstruct
    public void init() {
        database = client.getDatabase("admin");
    }

    @GET
    public Double getConnectionStatus() {
        Document connectionStatus = database.runCommand(new Document("connectionStatus", 1));
        return connectionStatus.getDouble("ok");
    }

}
