package io.quarkus.narayana.jta;

import javax.transaction.Transaction;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.arjuna.ats.jta.TransactionManager;

@Path("/hello")
public class TransactionalResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String hello() throws javax.transaction.SystemException {
        Transaction txn = TransactionManager.transactionManager().getTransaction();
        return String.valueOf(txn.getStatus());
    }
}
