package io.quarkus.narayana.jta;

import jakarta.inject.Inject;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/tx")
public class TransactionalResource {
    @Inject
    TransactionManager tm;

    @Path("/status")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String status() throws jakarta.transaction.SystemException {
        Transaction txn = tm.getTransaction();
        return String.valueOf(txn.getStatus());
    }
}
