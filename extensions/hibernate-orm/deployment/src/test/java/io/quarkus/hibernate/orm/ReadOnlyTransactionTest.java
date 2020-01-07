package io.quarkus.hibernate.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import io.quarkus.test.QuarkusUnitTest;

public class ReadOnlyTransactionTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Address.class)
                    .addAsResource("application.properties"));

    @Inject
    EntityManager entityManager;

    @BeforeEach
    @Transactional
    void init() {
        Address adr = new Address();
        adr.setStreet("rue de Paris");
        entityManager.persist(adr);
        entityManager.flush();
    }

    @AfterEach
    @Transactional
    void destroy() {
        int deleted = entityManager.createQuery("delete from Address where street = 'rue de Paris'").executeUpdate();
        assertEquals(1, deleted);
        entityManager.flush();
    }

    @Test
    @Transactional
    @TransactionConfiguration(readOnly = true)
    public void testRO() {
        TypedQuery<Address> query = entityManager.createQuery("from Address where street = 'rue de Paris'", Address.class);
        Address result = query.getSingleResult();
        assertNotNull(result);

        Session session = entityManager.unwrap(Session.class);
        assertTrue(session.isDefaultReadOnly());
        assertEquals(FlushMode.MANUAL, session.getHibernateFlushMode());

        assertThrows(Exception.class, () -> forceRO());
    }

    @Transactional
    @TransactionConfiguration(readOnly = false)
    public void forceRO() {
        Address adr = new Address();
        adr.setStreet("rue du paradis");
        entityManager.persist(adr);
        entityManager.flush();
    }

    @Test
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @TransactionConfiguration(readOnly = true)
    public void testSubTransactions() {
        TypedQuery<Address> query = entityManager.createQuery("from Address where street = 'rue de Paris'", Address.class);
        Address result = query.getSingleResult();
        assertNotNull(result);

        System.out.println(entityManager);

        Session session = entityManager.unwrap(Session.class);
        assertTrue(session.isDefaultReadOnly());
        assertEquals(FlushMode.MANUAL, session.getHibernateFlushMode());

        // don't know what will happen here ...
        newTransaction();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @TransactionConfiguration(readOnly = false)
    public void newTransaction() {
        Session session = entityManager.unwrap(Session.class);
        assertFalse(session.isDefaultReadOnly());
        assertEquals(FlushMode.AUTO, session.getHibernateFlushMode());

        Address adr = new Address();
        adr.setStreet("rue du paradis");
        entityManager.persist(adr);
        entityManager.flush();
    }
}
