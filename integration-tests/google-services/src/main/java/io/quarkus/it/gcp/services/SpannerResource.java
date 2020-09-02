package io.quarkus.it.gcp.services;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.cloud.spanner.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.jboss.logging.Logger;

@Path("/spanner")
public class SpannerResource {
    private static final Logger LOG = Logger.getLogger(SpannerResource.class);

    @Inject Spanner spanner;

    @ConfigProperty(name="quarkus.google.cloud.project-id") String projectId;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String spanner() {
        DatabaseId id = DatabaseId.of(projectId, "test-instance", "test-database");
        DatabaseClient dbClient = spanner.getDatabaseClient(id);

        // Insert 4 singer records
        dbClient.readWriteTransaction().run(transaction -> {
            String sql =
                    "INSERT INTO Singers (SingerId, FirstName, LastName) VALUES "
                            + "(12, 'Melissa', 'Garcia'), "
                            + "(13, 'Russell', 'Morales'), "
                            + "(14, 'Jacqueline', 'Long'), "
                            + "(15, 'Dylan', 'Shaw')";
            long rowCount = transaction.executeUpdate(Statement.of(sql));
            LOG.infov("{0} records inserted.", rowCount);
            return null;
        });

        // read them
        try (ResultSet resultSet = dbClient.singleUse() // Execute a single read or query against Cloud Spanner.
                    .executeQuery(Statement.of("SELECT SingerId, FirstName, LastName FROM Singers"))) {
            StringBuilder builder = new StringBuilder();
            while (resultSet.next()) {
                builder.append(resultSet.getLong(0)).append(' ').
                        append(resultSet.getString(1)).append(' ')
                        .append(resultSet.getString(2)).append('\n');
            }
            return builder.toString();
        }
    }

}