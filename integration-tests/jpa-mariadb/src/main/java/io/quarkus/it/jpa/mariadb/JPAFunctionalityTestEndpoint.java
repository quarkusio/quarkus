package io.quarkus.it.jpa.mariadb;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

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
 * Basic test running JPA with the MariaDB database.
 * The application can work in either standard JVM or in native mode.
 */
@Path("/jpa-mariadb/testfunctionality")
@Produces(MediaType.TEXT_PLAIN)
public class JPAFunctionalityTestEndpoint {

    @Inject
    EntityManager em;

    @Inject
    DataSource ds;

    @GET
    public String test() throws IOException, SQLException {
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

        //Try to use prepared statement with setObject:
        try (PreparedStatement ps = ds.getConnection().prepareStatement("select * from Person as p where p.name = ?")) {
            ps.setObject(1, "Quarkus");
            final ResultSet resultSet = ps.executeQuery();

            if (!resultSet.next()) {
                throw new RuntimeException("Person Quarkus doesn't exist when it should");
            }
        }

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
        person.setAddress(new Address("Street " + randomName()));
        em.persist(person);
    }

    private static String randomName() {
        return UUID.randomUUID().toString();
    }
}
