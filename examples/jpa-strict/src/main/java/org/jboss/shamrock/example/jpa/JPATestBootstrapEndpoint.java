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
 * Various tests for the JPA integration. WARNING: these tests will ONLY pass in Substrate, as it also verifies
 * reflection non-functionality.
 */
@WebServlet(name = "JPATestBootstrapEndpoint", urlPatterns = "/jpa/testbootstrap")
public class JPATestBootstrapEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            testStoreLoadOnJPA();
        } catch (Exception e) {
            reportException("Oops, shit happened, No boot for you!", e, resp);
        }

        resp.getWriter().write("OK");
    }

    public void testStoreLoadOnJPA() throws Exception {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("templatePU");
        System.out.println("Hibernate EntityManagerFactory: booted");

        doStuffWithHibernate(entityManagerFactory);

        entityManagerFactory.close();
        System.out.println("Hibernate EntityManagerFactory: shut down");

    }

    private static void doStuffWithHibernate(EntityManagerFactory entityManagerFactory) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        persistNewPerson(em);

        listExistingPersons(em);

        transaction.commit();
        em.close();
    }

    private static void listExistingPersons(EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Person> cq = cb.createQuery(Person.class);
        Root<Person> from = cq.from(Person.class);
        cq.select(from);
        TypedQuery<Person> q = em.createQuery(cq);
        List<Person> allpersons = q.getResultList();
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

    private static void persistNewPerson(EntityManager entityManager) {
        Person person = new Person();
        person.setName(randomName());
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
