package io.quarkus.it.jpa.postgresql;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.it.jpa.postgresql.otherpu.EntityWithJsonOtherPU;

/**
 * Various tests covering JPA functionality. All tests should work in both standard JVM and in native mode.
 */
@WebServlet(name = "JPATestBootstrapEndpoint", urlPatterns = "/jpa/testfunctionality")
public class JPAFunctionalityTestEndpoint extends HttpServlet {

    @Inject
    EntityManagerFactory entityManagerFactory;
    @Inject
    @PersistenceUnit("other")
    EntityManagerFactory otherEntityManagerFactory;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            doStuffWithHibernate(entityManagerFactory, otherEntityManagerFactory);
        } catch (Exception e) {
            reportException("An error occurred while performing Hibernate operations", e, resp);
        }
        resp.getWriter().write("OK");
    }

    /**
     * Lists the various operations we want to test for:
     */
    private static void doStuffWithHibernate(EntityManagerFactory entityManagerFactory,
            EntityManagerFactory otherEntityManagerFactory) {

        //Cleanup any existing data:
        deleteAllPerson(entityManagerFactory);

        //Store some well known Person instances we can then test on:
        storeTestPersons(entityManagerFactory);

        //Load all persons and run some checks on the query results:
        verifyListOfExistingPersons(entityManagerFactory);

        //Try a JPA named query:
        verifyJPANamedQuery(entityManagerFactory);

        deleteAllPerson(entityManagerFactory);

        // Try an entity using a UUID
        verifyUUIDEntity(entityManagerFactory);

        doJsonStuff(entityManagerFactory, otherEntityManagerFactory);

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
        em.createNativeQuery("Delete from myschema.Person").executeUpdate();
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
            sb.append("\n\t");
            if (p.getStatus() != Status.LIVING) {
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
        person.setLatestLunchBreakDuration(Duration.ofMinutes(30));
        entityManager.persist(person);
    }

    private static String randomName() {
        return UUID.randomUUID().toString();
    }

    private static void verifyUUIDEntity(final EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        MyUUIDEntity myEntity = new MyUUIDEntity();
        myEntity.setName("George");
        em.persist(myEntity);
        transaction.commit();
        em.close();

        em = emf.createEntityManager();
        transaction = em.getTransaction();
        transaction.begin();
        myEntity = em.find(MyUUIDEntity.class, myEntity.getId());
        if (myEntity == null || !"George".equals(myEntity.getName())) {
            throw new RuntimeException("Incorrect loaded MyUUIDEntity " + myEntity);
        }
        transaction.commit();
        em.close();
    }

    private static void doJsonStuff(EntityManagerFactory emf, EntityManagerFactory otherEmf) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();

            EntityWithJson entity = new EntityWithJson(
                    new EntityWithJson.ToBeSerializedWithDateTime(LocalDate.of(2023, 7, 28)));
            em.persist(entity);
            transaction.commit();

            transaction.begin();
            List<EntityWithJson> entities = em
                    .createQuery("select e from EntityWithJson e", EntityWithJson.class)
                    .getResultList();
            if (entities.isEmpty()) {
                throw new AssertionError("No entities with json were found");
            }
            transaction.commit();

            transaction.begin();
            em.createQuery("delete from EntityWithJson").executeUpdate();
            transaction.commit();
        }

        try (EntityManager em = otherEmf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            EntityWithJsonOtherPU otherPU = new EntityWithJsonOtherPU(
                    new EntityWithJsonOtherPU.ToBeSerializedWithDateTime(LocalDate.of(2023, 7, 28)));
            em.persist(otherPU);
            transaction.commit();
            throw new AssertionError(
                    "Default mapper cannot process date/time properties. So we were expecting commit to fail, but it did not!");
        } catch (Exception e) {
            if (!(e.getCause() instanceof IllegalArgumentException)
                    && !e.getCause().getMessage().contains("I cannot convert anything to JSON")) {
                throw new AssertionError("Transaction failed for a different reason than expected.", e);
            }
        }
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
