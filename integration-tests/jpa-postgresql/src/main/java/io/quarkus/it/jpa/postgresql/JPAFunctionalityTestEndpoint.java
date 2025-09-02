package io.quarkus.it.jpa.postgresql;

import java.time.LocalDate;
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

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.it.jpa.postgresql.defaultpu.EntityWithJson;
import io.quarkus.it.jpa.postgresql.defaultpu.MyUUIDEntity;
import io.quarkus.it.jpa.postgresql.defaultpu.Person;
import io.quarkus.it.jpa.postgresql.defaultpu.SequencedAddress;
import io.quarkus.it.jpa.postgresql.defaultpu.SomeEmbeddable;
import io.quarkus.it.jpa.postgresql.otherpu.EntityWithJsonOtherPU;
import io.quarkus.narayana.jta.QuarkusTransaction;

/**
 * Various tests covering JPA functionality. All tests should work in both standard JVM and in native mode.
 */
@Path("/jpa/testfunctionality")
@Produces(MediaType.TEXT_PLAIN)
public class JPAFunctionalityTestEndpoint {

    @Inject
    EntityManager em;

    @Inject
    @PersistenceUnit("other")
    EntityManager otherEm;

    @GET
    @Path("base")
    public String base() {
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
                .run(() -> em.createNativeQuery("Delete from myschema.Person").executeUpdate());
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

    @GET
    @Path("uuid")
    public String uuid() {
        var id = QuarkusTransaction.requiringNew().call(() -> {
            MyUUIDEntity myEntity = new MyUUIDEntity();
            myEntity.setName("George");
            em.persist(myEntity);
            return myEntity.getId();
        });

        QuarkusTransaction.requiringNew().run(() -> {
            var myEntity = em.find(MyUUIDEntity.class, id);
            if (myEntity == null || !"George".equals(myEntity.getName())) {
                throw new RuntimeException("Incorrect loaded MyUUIDEntity " + myEntity);
            }
        });
        return "OK";
    }

    @GET
    @Path("json")
    public String json() {
        QuarkusTransaction.requiringNew().run(() -> {
            EntityWithJson entity = new EntityWithJson(
                    new EntityWithJson.ToBeSerializedWithDateTime(LocalDate.of(2023, 7, 28)),
                    new SomeEmbeddable(100, LocalDate.of(2023, 7, 29)));
            em.persist(entity);
        });

        QuarkusTransaction.requiringNew().run(() -> {
            List<EntityWithJson> entities = em
                    .createQuery("select e from EntityWithJson e", EntityWithJson.class)
                    .getResultList();
            if (entities.isEmpty()) {
                throw new AssertionError("No entities with json were found");
            }
        });

        QuarkusTransaction.requiringNew().run(() -> {
            em.createQuery("delete from EntityWithJson").executeUpdate();
        });

        Exception exception = null;
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                EntityWithJsonOtherPU otherPU = new EntityWithJsonOtherPU(
                        new EntityWithJsonOtherPU.ToBeSerializedWithDateTime(LocalDate.of(2023, 7, 28)));
                otherEm.persist(otherPU);
            });
        } catch (Exception e) {
            exception = e;
        }

        if (exception == null) {
            throw new AssertionError(
                    "Default mapper cannot process date/time properties. So we were expecting transaction to fail, but it did not!");
        }
        if (!(exception instanceof UnsupportedOperationException)
                || !exception.getMessage().contains("I cannot convert anything to JSON")) {
            throw new AssertionError("flush failed for a different reason than expected.", exception);
        }

        return "OK";
    }

}
