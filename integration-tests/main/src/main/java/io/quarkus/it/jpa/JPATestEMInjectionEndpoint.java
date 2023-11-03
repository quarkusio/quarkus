package io.quarkus.it.jpa;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.UserTransaction;

/**
 * Various tests for the JPA integration.
 */
@WebServlet(urlPatterns = "/jpa/testjpaeminjection")
public class JPATestEMInjectionEndpoint extends HttpServlet {

    @Inject
    EntityManager em;

    @Inject
    UserTransaction transaction;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            testStoreLoadOnJPA();
        } catch (Exception e) {
            reportException("An error occurred while performing Hibernate operations", e, resp);
        }
        resp.getWriter().write("OK");
    }

    public void testStoreLoadOnJPA() throws Exception {
        doStuffWithHibernate();

    }

    private void doStuffWithHibernate() throws Exception {
        transaction.begin();

        persistNewPerson(em);

        listExistingPersons(em);

        transaction.commit();
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
        }
        sb.append("\nList complete.\n");
        System.out.print(sb);
    }

    private static void persistNewPerson(EntityManager entityManager) {
        Person person = new Person();
        person.setName(randomName());
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
