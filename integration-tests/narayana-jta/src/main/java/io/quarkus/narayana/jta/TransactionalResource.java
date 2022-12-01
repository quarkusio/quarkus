package io.quarkus.narayana.jta;

import javax.inject.Inject;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/tx")
public class TransactionalResource {
    @Inject
    TransactionManager tm;

    @Path("/status")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String status() throws javax.transaction.SystemException {
        Transaction txn = tm.getTransaction();
        return String.valueOf(txn.getStatus());
    }
}
