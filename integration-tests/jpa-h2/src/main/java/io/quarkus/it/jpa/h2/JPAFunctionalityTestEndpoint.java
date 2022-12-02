package io.quarkus.it.jpa.h2;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wildfly.common.Assert;

/**
 * Basic test running JPA with the H2 database.
 * The application can work in either standard JVM or in native mode, while we run H2 as a separate JVM process.
 */
@WebServlet(name = "JPATestBootstrapEndpoint", urlPatterns = "/jpa-h2/testfunctionality")
public class JPAFunctionalityTestEndpoint extends HttpServlet {

    @PersistenceUnit(unitName = "templatePU")
    EntityManagerFactory entityManagerFactory;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            doStuffWithHibernate(entityManagerFactory);
        } catch (Exception e) {
            reportException("An error occurred while performing Hibernate operations", e, resp);
        }
        resp.getWriter().write("OK");
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

        verifyHqlFetch(entityManagerFactory);

        deleteAllPerson(entityManagerFactory);

        //Test capability to load enhanced proxies:
        verifyEnhancedProxies(entityManagerFactory);

    }

    private static void verifyEnhancedProxies(EntityManagerFactory emf) {
        //Define the test data:
        CompanyCustomer company = new CompanyCustomer();
        company.companyname = "Quarked consulting, inc.";
        Project project = new Project();
        project.name = "Hibernate RX";
        project.customer = company;

        //Store the test model:
        doAsUnit(emf, em -> em.persist(project));
        final Integer testId = project.id;
        Assert.assertNotNull(testId);

        //Now try to load it, should trigger the use of enhanced proxies:
        doAsUnit(emf, em -> em.find(Project.class, testId));
    }

    private static void verifyHqlFetch(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                em.createQuery("from Person p left join fetch p.address a").getResultList();

                transaction.commit();
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        } finally {
            em.close();
        }
    }

    private static void verifyJPANamedQuery(final EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        TypedQuery<Person> typedQuery = em.createNamedQuery(
                "get_person_by_name", Person.class);
        typedQuery.setParameter("name", "Quarkus");
        final Person singleResult = typedQuery.getSingleResult();

        if (!singleResult.getName().equals("Quarkus")) {
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
        persistNewPerson(em, "Gizmo");
        persistNewPerson(em, "Quarkus");
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
        if (allpersons.size() != 3) {
            throw new RuntimeException("Incorrect number of results");
        }
        if (!allpersons.get(0).getName().equals("Gizmo")) {
            throw new RuntimeException("Incorrect order of results");
        }
        StringBuilder sb = new StringBuilder("list of stored Person names:\n\t");
        for (Person p : allpersons) {
            p.describeFully(sb);
        }
        sb.append("\nList complete.\n");
        System.out.print(sb);
    }

    private static void persistNewPerson(EntityManager entityManager, String name) {
        Person person = new Person();
        person.setName(name);
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

    private static void doAsUnit(EntityManagerFactory emf, Consumer<EntityManager> f) {
        final EntityManager em = emf.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                f.accept(em);
                transaction.commit();
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        } finally {
            em.close();
        }
    }

}
