package org.jboss.shamrock.example.test;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.jboss.shamrock.example.jpa.Person;
import org.jboss.shamrock.example.jpa.SequencedAddress;
import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 */
@RunWith(ShamrockTest.class)
public class JPAStoreLoadTestCase {

    @Test
    public void testStoreLoadOnJPA() throws Exception {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory( "templatePU" );
        System.out.println( "Hibernate EntityManagerFactory: booted" );

        doStuffWithHibernate( entityManagerFactory );

        entityManagerFactory.close();
        System.out.println( "Hibernate EntityManagerFactory: shut down" );

    }


    private static void doStuffWithHibernate(EntityManagerFactory entityManagerFactory) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        persistNewPerson( em );

        listExistingPersons( em );

        transaction.commit();
        em.close();
    }

    private static void listExistingPersons(EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Person> cq = cb.createQuery( Person.class );
        Root<Person> from = cq.from( Person.class );
        cq.select( from );
        TypedQuery<Person> q = em.createQuery( cq );
        List<Person> allpersons = q.getResultList();
        StringBuilder sb = new StringBuilder( "list of stored Person names:\n\t" );
        for ( Person p : allpersons ) {
            p.describeFully( sb );
            sb.append( "\n\t" );
        }
        sb.append( "\nList complete.\n" );
        System.out.print( sb );
    }

    private static void persistNewPerson(EntityManager entityManager) {
        Person person = new Person();
        person.setName( randomName() );
        person.setAddress( new SequencedAddress( "Street " + randomName() ) );
        entityManager.persist( person );
    }

    private static String randomName() {
        return UUID.randomUUID().toString();
    }


}
