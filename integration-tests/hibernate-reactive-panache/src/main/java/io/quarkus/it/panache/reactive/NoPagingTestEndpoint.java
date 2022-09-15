package io.quarkus.it.panache.reactive;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;

/**
 * Run a simple, no paged PanacheQueryTest in order to log the generated SQL.
 *
 * @see io.quarkus.it.panache.reactive.NoPagingPMT
 */
@Path("no-paging-test")
public class NoPagingTestEndpoint {

    @GET
    public Uni<String> test() {
        return Panache.withTransaction(() -> PageItem.findAll().list().map(v -> "OK"));
    }
}
