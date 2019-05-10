package io.quarkus.context.test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

@ApplicationScoped
public class TransactionalBean {

    @Inject
    TransactionManager tm;

    @Transactional(value = TxType.REQUIRES_NEW)
    public void doInTx() {
        try {
            System.err.println("service bean TX: " + tm.getTransaction());
            ContextEntity entity = new ContextEntity();
            entity.name = "Stef";
            entity.persist();

        } catch (SystemException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
