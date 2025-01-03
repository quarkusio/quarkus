package io.quarkus.it.opentelemetry;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.it.opentelemetry.model.Hit;
import io.quarkus.it.opentelemetry.model.db2.Db2Hit;
import io.quarkus.it.opentelemetry.model.h2.H2Hit;
import io.quarkus.it.opentelemetry.model.mariadb.MariaDbHit;
import io.quarkus.it.opentelemetry.model.oracle.OracleHit;
import io.quarkus.it.opentelemetry.model.pg.PgHit;

@ApplicationScoped
@Path("/")
public class PingPongResource {

    @Transactional
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/hit/{tenant}")
    public Hit createHit(@QueryParam("id") Long id, @PathParam("tenant") String tenant) {
        switch (tenant) {
            case "postgresql":
                persist(PgHit::new, id);
                return PgHit.findById(id);
            case "oracle":
                persist(OracleHit::new, id);
                return OracleHit.findById(id);
            case "mariadb":
                persist(MariaDbHit::new, id);
                return MariaDbHit.findById(id);
            case "db2":
                persist(Db2Hit::new, id);
                return Db2Hit.findById(id);
            case "h2":
                persist(H2Hit::new, id);
                return H2Hit.findById(id);
            default:
                throw new IllegalArgumentException();
        }
    }

    private void persist(Supplier<Hit> hitSupplier, Long id) {
        Hit hit = hitSupplier.get();
        hit.setId(id);
        hit.setMessage("Hit message.");
        hit.persist();
    }

}
