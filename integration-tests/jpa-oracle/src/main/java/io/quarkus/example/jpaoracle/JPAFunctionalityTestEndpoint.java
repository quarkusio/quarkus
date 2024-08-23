package io.quarkus.example.jpaoracle;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.narayana.jta.QuarkusTransaction;

/**
 * Basic test running JPA with the Oracle database.
 * The application can work in either standard JVM or SubstrateVM.
 */
@Path("/jpa-oracle/testfunctionality")
@Produces(MediaType.TEXT_PLAIN)
public class JPAFunctionalityTestEndpoint {

    @Inject
    EntityManager em;

    @GET
    public String test() throws IOException {
        cleanUpData();

        //Store some well known Person instances we can then test on:
        QuarkusTransaction.requiringNew().run(() -> {
            persistNewPerson("Gizmo");
            persistNewPerson("Quarkus");
            persistNewPerson("Hibernate ORM");
        });

        //Load all persons and run some checks on the query results:
        QuarkusTransaction.requiringNew().run(() -> {
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
        });

        //Try a JPA named query:
        QuarkusTransaction.requiringNew().run(() -> {
            TypedQuery<Person> typedQuery = em.createNamedQuery(
                    "get_person_by_name", Person.class);
            typedQuery.setParameter("name", "Quarkus");
            final Person singleResult = typedQuery.getSingleResult();

            if (!singleResult.getName().equals("Quarkus")) {
                throw new RuntimeException("Wrong result from named JPA query");
            }
        });

        //Check that HQL fetch does not throw an exception
        QuarkusTransaction.requiringNew()
                .run(() -> em.createQuery("from Person p left join fetch p.address a").getResultList());

        cleanUpData();

        return "OK";
    }

    private void cleanUpData() {
        QuarkusTransaction.requiringNew()
                .run(() -> em.createNativeQuery("Delete from Person").executeUpdate());
    }

    private void persistNewPerson(String name) {
        Person person = new Person();
        person.setName(name);
        person.setAddress(new SequencedAddress("Street " + randomName()));
        em.persist(person);
    }

    private static String randomName() {
        return UUID.randomUUID().toString();
    }

}
