package io.quarkus.it.panache;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Run a simple, no paged PanacheQueryTest in order to log the generated SQL.
 *
 * @see io.quarkus.it.panache.NoPagingPMT
 */
@Path("no-paging-test")
public class NoPagingTestEndpoint {

    @GET
    @Transactional
    public String test() {
        PageItem.findAll().list();

        return "OK";
    }
}
