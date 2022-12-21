package io.quarkus.it.opentelemetry;

import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.quarkus.it.opentelemetry.model.Hit;
import io.quarkus.it.opentelemetry.model.db2.Db2Hit;
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
