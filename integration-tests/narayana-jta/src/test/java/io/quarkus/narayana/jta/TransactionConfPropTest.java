package io.quarkus.narayana.jta;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.arjuna.ats.arjuna.common.arjPropertyManager;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TransactionConfPropTest {

    @Inject
    TransactionManager tm;

    /*
     * verify that the objectStore directory path for JTA can be configured
     */
    @Test
    void testObjectStoreDirPath() {
        // verify that the quarkus configuration took effect
        Assertions.assertEquals("target/tx-object-store", // this value is set via application.properties
                arjPropertyManager.getObjectStoreEnvironmentBean().getObjectStoreDir());
    }

    @Test
    public void testObjectStoreExist() throws Exception {

        tm.begin();
        assertTrue(tm.getTransaction().enlistResource(new XAResource() {

            @Override
            public void start(Xid arg0, int arg1) throws XAException {

            }

            @Override
            public boolean setTransactionTimeout(int arg0) throws XAException {
                return false;
            }

            @Override
            public void rollback(Xid arg0) throws XAException {

            }

            @Override
            public Xid[] recover(int arg0) throws XAException {
                return null;
            }

            @Override
            public int prepare(Xid arg0) throws XAException {
                return 0;
            }

            @Override
            public boolean isSameRM(XAResource arg0) throws XAException {
                return false;
            }

            @Override
            public int getTransactionTimeout() throws XAException {
                return 0;
            }

            @Override
            public void forget(Xid arg0) throws XAException {

            }

            @Override
            public void end(Xid arg0, int arg1) throws XAException {

            }

            @Override
            public void commit(Xid arg0, boolean arg1) throws XAException {

            }
        }));
        assertTrue(tm.getTransaction().enlistResource(new XAResource() {

            @Override
            public void start(Xid xid, int flags) throws XAException {

            }

            @Override
            public boolean setTransactionTimeout(int seconds) throws XAException {
                return false;
            }

            @Override
            public void rollback(Xid xid) throws XAException {

            }

            @Override
            public Xid[] recover(int flag) throws XAException {
                return null;
            }

            @Override
            public int prepare(Xid xid) throws XAException {
                return 0;
            }

            @Override
            public boolean isSameRM(XAResource xares) throws XAException {
                return false;
            }

            @Override
            public int getTransactionTimeout() throws XAException {
                return 0;
            }

            @Override
            public void forget(Xid xid) throws XAException {

            }

            @Override
            public void end(Xid xid, int flags) throws XAException {
            }

            @Override
            public void commit(Xid xid, boolean onePhase) throws XAException {

            }
        }));
        try {
            tm.commit();
        } catch (Exception e) {
            tm.rollback();
        }

        // checking if the object-store is present in expected location
        File f = new File("target/tx-object-store");
        assertTrue(f.exists());

    }
}
