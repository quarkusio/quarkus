package org.jboss.shamrock.example.jpa;

import java.io.IOException;
import java.io.PrintWriter;
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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Various tests convering JPA functionality. All tests should work in both standard JVM and SubstrateVM.
 */
@WebServlet(name = "JPATestBootstrapEndpoint", urlPatterns = "/jpa/testfunctionality")
public class JPAFunctionalityTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            bootAndDoStuff();
        } catch (Exception e) {
            reportException("Oops, shit happened, No boot for you!", e, resp);
        }
        resp.getWriter().write("OK");
    }

    public void bootAndDoStuff() throws Exception {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("templatePU");
        try {
            System.out.println("Hibernate EntityManagerFactory: booted");
            doStuffWithHibernate(entityManagerFactory);
        }
        finally {
            entityManagerFactory.close();
            System.out.println("Hibernate EntityManagerFactory: shut down");
        }
    }

    /**
     * Lists the various operations we want to test for:
     */
    private static void doStuffWithHibernate(EntityManagerFactory entityManagerFactory) {

        //Cleanup any existing data:
        deleteAllPerson(entityManagerFactory);

        //Store some well known Person instances we can then test on:
        storeTestPersons(entityManagerFactory);

        //Load all persons and run some checks on the query results:
        verifyListOfExistingPersons(entityManagerFactory);

        //Try a JPA named query:
        verifyJPANamedQuery(entityManagerFactory);

        deleteAllPerson(entityManagerFactory);

    }

    private static void verifyJPANamedQuery(final EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        TypedQuery<Person> typedQuery = em.createNamedQuery(
              "get_person_by_name", Person.class
        );
        typedQuery.setParameter("name", "Shamrock");
        final Person singleResult = typedQuery.getSingleResult();

        if ( ! singleResult.getName().equals("Shamrock")) {
            throw new RuntimeException("Wrong result from named JPA query");
        }

        transaction.commit();
        em.close();
    }

    private static void verifyListOfExistingPersons(final EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        listExistingPersons(em);
        transaction.commit();
        em.close();
    }

    private static void storeTestPersons(final EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        persistNewPerson(em, "Protean");
        persistNewPerson(em, "Shamrock");
        persistNewPerson(em, "Hibernate ORM");
        transaction.commit();
        em.close();
    }

    private static void deleteAllPerson(final EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        em.createNativeQuery("Delete from Person").executeUpdate();
        transaction.commit();
        em.close();
    }

    private static void listExistingPersons(EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Person> cq = cb.createQuery(Person.class);
        Root<Person> from = cq.from(Person.class);
        cq.select(from).orderBy(cb.asc(from.get("name")));
        TypedQuery<Person> q = em.createQuery(cq);
        List<Person> allpersons = q.getResultList();
        if ( allpersons.size() != 3 ) {
            throw new RuntimeException("Incorrect number of results");
        }
        if ( ! allpersons.get(0).getName().equals("Hibernate ORM") ) {
            throw new RuntimeException("Incorrect order of results");
        }
        StringBuilder sb = new StringBuilder("list of stored Person names:\n\t");
        for (Person p : allpersons) {
            p.describeFully(sb);
            sb.append("\n\t");
            if(p.getStatus() != Status.LIVING) {
                throw new RuntimeException("Incorrect status " + p);
            }
        }
        sb.append("\nList complete.\n");
        System.out.print(sb);
    }

    private static void persistNewPerson(EntityManager entityManager, String name) {
        Person person = new Person();
        person.setName(name);
        person.setStatus(Status.LIVING);
        person.setAddress(new SequencedAddress("Street " + randomName()));
        entityManager.persist(person);
    }

    private static String randomName() {
        return UUID.randomUUID().toString();
    }

    private void reportException(String errorMessage, final Exception e, final HttpServletResponse resp) throws IOException {
        final PrintWriter writer = resp.getWriter();
        if (errorMessage != null) {
            writer.write(errorMessage);
            writer.write(" ");
        }
        writer.write(e.toString());
        writer.append("\n\t");
        e.printStackTrace(writer);
        writer.append("\n\t");
    }

}
