package io.quarkus.it.arango;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.arangodb.ArangoDB;

@Path("/arango")
public class ArangoResource {

    @Inject
    ArangoDB driver;

    @GET
    @Path("version")
    public String getVersion() {
        String version = null;
        try {
            version = driver.getVersion().getVersion();
        } catch (Exception e) {
        }
        return version;
    }

    @GET
    @Path("db")
    public String doStuffWithNeo4j() {
        String db = null;
        try {

            String sampleDbName = "db";
            driver.createDatabase(sampleDbName);
            db = driver.db(sampleDbName).name();
        } catch (Exception e) {
        }
        return db;
    }
}
