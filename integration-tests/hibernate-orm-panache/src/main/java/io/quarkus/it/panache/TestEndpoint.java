package io.quarkus.it.panache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.jpa.QueryHints;
import org.junit.jupiter.api.Assertions;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.exception.PanacheQueryException;

/**
 * Various tests covering Panache functionality. All tests should work in both standard JVM and in native mode.
 */
@Path("test")
public class TestEndpoint {

    // fake unused injection point to force ArC to not remove this otherwise I can't mock it in the tests
    @Inject
    MockablePersonRepository mockablePersonRepository;

    @GET
    @Path("model")
    @Transactional
    public String testModel() {
        Person.flush();
        Assertions.assertNotNull(Person.getEntityManager());

        List<Person> persons = Person.findAll().list();
        Assertions.assertEquals(0, persons.size());

        persons = Person.listAll();
        Assertions.assertEquals(0, persons.size());

        Stream<Person> personStream = Person.findAll().stream();
        Assertions.assertEquals(0, personStream.count());

        personStream = Person.streamAll();
        Assertions.assertEquals(0, personStream.count());

        try {
            Person.findAll().singleResult();
            Assertions.fail("singleResult should have thrown");
        } catch (NoResultException x) {
        }

        Assertions.assertNull(Person.findAll().firstResult());

        Person person = makeSavedPerson();
        Assertions.assertNotNull(person.id);

        Assertions.assertEquals(1, Person.count());
        Assertions.assertEquals(1, Person.count("name = ?1", "stef"));
        Assertions.assertEquals(1, Person.count("name = :name", Parameters.with("name", "stef").map()));
        Assertions.assertEquals(1, Person.count("name = :name", Parameters.with("name", "stef")));
        Assertions.assertEquals(1, Person.count("name", "stef"));

        Assertions.assertEquals(1, Dog.count());
        Assertions.assertEquals(1, person.dogs.size());

        persons = Person.findAll().list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = Person.listAll();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        personStream = Person.findAll().stream();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        personStream = Person.streamAll();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        Assertions.assertEquals(person, Person.findAll().firstResult());
        Assertions.assertEquals(person, Person.findAll().singleResult());

        persons = Person.find("name = ?1", "stef").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        // full form
        persons = Person.find("FROM Person2 WHERE name = ?1", "stef").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = Person.find("name = ?1", "stef").withLock(LockModeType.PESSIMISTIC_READ).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        // next calls to this query will be cached
        persons = Person.find("name = ?1", "stef").withHint(QueryHints.HINT_CACHEABLE, "true").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = Person.list("name = ?1", "stef");
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = Person.find("name = :name", Parameters.with("name", "stef").map()).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = Person.find("name = :name", Parameters.with("name", "stef")).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = Person.list("name = :name", Parameters.with("name", "stef").map());
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = Person.list("name = :name", Parameters.with("name", "stef"));
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = Person.find("name", "stef").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        personStream = Person.find("name = ?1", "stef").stream();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        personStream = Person.stream("name = ?1", "stef");
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        personStream = Person.stream("name = :name", Parameters.with("name", "stef").map());
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        personStream = Person.stream("name = :name", Parameters.with("name", "stef"));
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        personStream = Person.find("name", "stef").stream();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        Assertions.assertEquals(person, Person.find("name", "stef").firstResult());
        Assertions.assertEquals(person, Person.find("name", "stef").singleResult());

        //named query
        persons = Person.find("#Person.getByName", Parameters.with("name", "stef")).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));
        Assertions.assertEquals(1, Person.find("#Person.getByName", Parameters.with("name", "stef")).count());
        Assertions.assertThrows(PanacheQueryException.class, () -> Person.find("#Person.namedQueryNotFound").list());
        NamedQueryEntity.find("#NamedQueryMappedSuperClass.getAll").list();
        NamedQueryEntity.find("#NamedQueryEntity.getAll").list();
        NamedQueryWith2QueriesEntity.find("#NamedQueryWith2QueriesEntity.getAll1").list();
        NamedQueryWith2QueriesEntity.find("#NamedQueryWith2QueriesEntity.getAll2").list();

        //empty query
        persons = Person.find("").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));
        persons = Person.find(null).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        Person byId = Person.findById(person.id);
        Assertions.assertEquals(person, byId);
        Assertions.assertEquals("Person<" + person.id + ">", byId.toString());

        byId = Person.<Person> findByIdOptional(person.id).get();
        Assertions.assertEquals(person, byId);
        Assertions.assertEquals("Person<" + person.id + ">", byId.toString());

        byId = Person.findById(person.id, LockModeType.PESSIMISTIC_READ);
        Assertions.assertEquals(person, byId);
        Assertions.assertEquals("Person<" + person.id + ">", byId.toString());

        byId = Person.<Person> findByIdOptional(person.id, LockModeType.PESSIMISTIC_READ).get();
        Assertions.assertEquals(person, byId);
        Assertions.assertEquals("Person<" + person.id + ">", byId.toString());

        person.delete();
        Assertions.assertEquals(0, Person.count());

        person = makeSavedPerson();
        Assertions.assertEquals(1, Person.count());
        Assertions.assertEquals(0, Person.delete("name = ?1", "emmanuel"));
        Assertions.assertEquals(1, Dog.delete("owner = ?1", person));
        Assertions.assertEquals(1, Person.delete("name", "stef"));
        person = makeSavedPerson();
        Assertions.assertEquals(1, Dog.delete("owner = :owner", Parameters.with("owner", person).map()));
        Assertions.assertEquals(1, Person.delete("name", "stef"));
        person = makeSavedPerson();
        Assertions.assertEquals(1, Dog.delete("owner = :owner", Parameters.with("owner", person)));
        Assertions.assertEquals(1, Person.delete("name", "stef"));
        // full form
        person = makeSavedPerson();
        Assertions.assertEquals(1, Dog.delete("FROM Dog WHERE owner = :owner", Parameters.with("owner", person)));
        Assertions.assertEquals(1, Person.delete("FROM Person2 WHERE name = ?1", "stef"));
        person = makeSavedPerson();
        Assertions.assertEquals(1, Dog.delete("DELETE FROM Dog WHERE owner = :owner", Parameters.with("owner", person)));
        Assertions.assertEquals(1, Person.delete("DELETE FROM Person2 WHERE name = ?1", "stef"));

        Assertions.assertEquals(0, Person.deleteAll());

        makeSavedPerson();
        Assertions.assertEquals(1, Dog.deleteAll());
        Assertions.assertEquals(1, Person.deleteAll());

        testPersist(PersistTest.Iterable);
        testPersist(PersistTest.Stream);
        testPersist(PersistTest.Variadic);
        Assertions.assertEquals(6, Person.deleteAll());

        testSorting();

        // paging
        for (int i = 0; i < 7; i++) {
            makeSavedPerson(String.valueOf(i));
        }
        testPaging(Person.findAll());
        testPaging(Person.find("ORDER BY name"));

        // range
        testRange(Person.findAll());
        testRange(Person.find("ORDER BY name"));

        try {
            Person.findAll().singleResult();
            Assertions.fail("singleResult should have thrown");
        } catch (NonUniqueResultException x) {
        }

        Assertions.assertNotNull(Person.findAll().firstResult());

        Assertions.assertNotNull(Person.findAll().firstResultOptional().get());

        Assertions.assertEquals(7, Person.deleteAll());

        testUpdate();

        //delete by id
        Person toRemove = new Person();
        toRemove.name = "testDeleteById";
        toRemove.uniqueName = "testDeleteByIdUnique";
        toRemove.persist();
        assertTrue(Person.deleteById(toRemove.id));
        Person.deleteById(666L); //not existing

        // persistAndFlush
        Person person1 = new Person();
        person1.name = "testFLush1";
        person1.uniqueName = "unique";
        person1.persist();
        Person person2 = new Person();
        person2.name = "testFLush2";
        person2.uniqueName = "unique";
        try {
            person2.persistAndFlush();
            Assertions.fail();
        } catch (PersistenceException pe) {
            //this is expected
        }

        return "OK";
    }

    private void testUpdate() {
        makeSavedPerson("p1");
        makeSavedPerson("p2");

        // full form
        int updateByIndexParameter = Person.update("update from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1");
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

        int updateByNamedParameter = Person.update("update from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                Parameters.with("pName", "stefp2").map());
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

        Assertions.assertEquals(2, Person.deleteAll());

        makeSavedPerson("p1");
        makeSavedPerson("p2");

        // less full form
        updateByIndexParameter = Person.update("from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1");
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

        updateByNamedParameter = Person.update("from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                Parameters.with("pName", "stefp2").map());
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

        Assertions.assertEquals(2, Person.deleteAll());

        makeSavedPerson("p1");
        makeSavedPerson("p2");

        updateByIndexParameter = Person.update("set name = 'stefNEW' where name = ?1", "stefp1");
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

        updateByNamedParameter = Person.update("set name = 'stefNEW' where name = :pName",
                Parameters.with("pName", "stefp2").map());
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

        Assertions.assertEquals(2, Person.deleteAll());

        makeSavedPerson("p1");
        makeSavedPerson("p2");

        updateByIndexParameter = Person.update("name = 'stefNEW' where name = ?1", "stefp1");
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

        updateByNamedParameter = Person.update("name = 'stefNEW' where name = :pName",
                Parameters.with("pName", "stefp2").map());
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

        Assertions.assertEquals(2, Person.deleteAll());

        makeSavedPerson("p1");
        makeSavedPerson("p2");

        updateByIndexParameter = Person.update("name = 'stefNEW' where name = ?1", "stefp1");
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

        updateByNamedParameter = Person.update("name = 'stefNEW' where name = :pName",
                Parameters.with("pName", "stefp2"));
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

        Assertions.assertEquals(2, Person.deleteAll());

        Assertions.assertThrows(PanacheQueryException.class, () -> Person.update(null),
                "PanacheQueryException should have thrown");

        Assertions.assertThrows(PanacheQueryException.class, () -> Person.update(" "),
                "PanacheQueryException should have thrown");

    }

    private void testUpdateDAO() {
        makeSavedPerson("p1");
        makeSavedPerson("p2");

        // full form
        int updateByIndexParameter = personDao.update("update from Person2 p set p.name = 'stefNEW' where p.name = ?1",
                "stefp1");
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

        int updateByNamedParameter = personDao.update("update from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                Parameters.with("pName", "stefp2").map());
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

        Assertions.assertEquals(2, personDao.deleteAll());

        makeSavedPerson("p1");
        makeSavedPerson("p2");

        // less full form
        updateByIndexParameter = personDao.update("from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1");
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

        updateByNamedParameter = personDao.update("from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                Parameters.with("pName", "stefp2").map());
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

        Assertions.assertEquals(2, personDao.deleteAll());

        makeSavedPerson("p1");
        makeSavedPerson("p2");

        updateByIndexParameter = personDao.update("set name = 'stefNEW' where name = ?1", "stefp1");
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

        updateByNamedParameter = personDao.update("set name = 'stefNEW' where name = :pName",
                Parameters.with("pName", "stefp2").map());
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

        Assertions.assertEquals(2, personDao.deleteAll());

        makeSavedPerson("p1");
        makeSavedPerson("p2");

        updateByIndexParameter = personDao.update("name = 'stefNEW' where name = ?1", "stefp1");
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

        updateByNamedParameter = personDao.update("name = 'stefNEW' where name = :pName",
                Parameters.with("pName", "stefp2").map());
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

        Assertions.assertEquals(2, personDao.deleteAll());

        makeSavedPerson("p1");
        makeSavedPerson("p2");

        updateByIndexParameter = personDao.update("name = 'stefNEW' where name = ?1", "stefp1");
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

        updateByNamedParameter = personDao.update("name = 'stefNEW' where name = :pName",
                Parameters.with("pName", "stefp2"));
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

        Assertions.assertEquals(2, personDao.deleteAll());

        Assertions.assertThrows(PanacheQueryException.class, () -> personDao.update(null),
                "PanacheQueryException should have thrown");

        Assertions.assertThrows(PanacheQueryException.class, () -> personDao.update(" "),
                "PanacheQueryException should have thrown");
    }

    private void testSorting() {
        Person person1 = new Person();
        person1.name = "stef";
        person1.status = Status.LIVING;
        person1.persist();

        Person person2 = new Person();
        person2.name = "stef";
        person2.status = Status.DECEASED;
        person2.persist();

        Person person3 = new Person();
        person3.name = "emmanuel";
        person3.status = Status.LIVING;
        person3.persist();

        Sort sort1 = Sort.by("name", "status");
        List<Person> order1 = Arrays.asList(person3, person2, person1);

        List<Person> list = Person.findAll(sort1).list();
        Assertions.assertEquals(order1, list);

        list = Person.listAll(sort1);
        Assertions.assertEquals(order1, list);

        list = Person.<Person> streamAll(sort1).collect(Collectors.toList());
        Assertions.assertEquals(order1, list);

        Sort sort2 = Sort.descending("name", "status");
        List<Person> order2 = Arrays.asList(person1, person2);

        list = Person.find("name", sort2, "stef").list();
        Assertions.assertEquals(order2, list);

        list = Person.list("name", sort2, "stef");
        Assertions.assertEquals(order2, list);

        list = Person.<Person> stream("name", sort2, "stef").collect(Collectors.toList());
        Assertions.assertEquals(order2, list);

        list = Person.find("name = :name", sort2, Parameters.with("name", "stef").map()).list();
        Assertions.assertEquals(order2, list);

        list = Person.list("name = :name", sort2, Parameters.with("name", "stef").map());
        Assertions.assertEquals(order2, list);

        list = Person.<Person> stream("name = :name", sort2, Parameters.with("name", "stef").map())
                .collect(Collectors.toList());
        Assertions.assertEquals(order2, list);

        list = Person.find("name = :name", sort2, Parameters.with("name", "stef")).list();
        Assertions.assertEquals(order2, list);

        list = Person.list("name = :name", sort2, Parameters.with("name", "stef"));
        Assertions.assertEquals(order2, list);

        list = Person.<Person> stream("name = :name", sort2, Parameters.with("name", "stef")).collect(Collectors.toList());
        Assertions.assertEquals(order2, list);

        Assertions.assertEquals(3, Person.deleteAll());
    }

    private Person makeSavedPerson(String suffix) {
        Person person = new Person();
        person.name = "stef" + suffix;
        person.status = Status.LIVING;
        person.address = new Address("stef street");
        person.address.persist();

        person.persist();
        return person;
    }

    private Person makeSavedPerson() {
        Person person = makeSavedPerson("");

        Dog dog = new Dog("octave", "dalmatian");
        dog.owner = person;
        person.dogs.add(dog);
        dog.persist();

        return person;
    }

    private void testPersist(PersistTest persistTest) {
        Person person1 = new Person();
        person1.name = "stef1";
        Person person2 = new Person();
        person2.name = "stef2";
        assertFalse(person1.isPersistent());
        assertFalse(person2.isPersistent());
        switch (persistTest) {
            case Iterable:
                Person.persist(Arrays.asList(person1, person2));
                break;
            case Stream:
                Person.persist(Stream.of(person1, person2));
                break;
            case Variadic:
                Person.persist(person1, person2);
                break;
        }
        assertTrue(person1.isPersistent());
        assertTrue(person2.isPersistent());
    }

    @Inject
    PersonRepository personDao;
    @Inject
    DogDao dogDao;
    @Inject
    AddressDao addressDao;
    @Inject
    NamedQueryRepository namedQueryRepository;
    @Inject
    NamedQueryWith2QueriesRepository namedQueryWith2QueriesRepository;

    @GET
    @Path("model-dao")
    @Transactional
    public String testModelDao() {
        personDao.flush();
        Assertions.assertNotNull(personDao.getEntityManager());

        List<Person> persons = personDao.findAll().list();
        Assertions.assertEquals(0, persons.size());

        Stream<Person> personStream = personDao.findAll().stream();
        Assertions.assertEquals(0, personStream.count());

        try {
            personDao.findAll().singleResult();
            Assertions.fail("singleResult should have thrown");
        } catch (NoResultException x) {
        }

        assertFalse(personDao.findAll().singleResultOptional().isPresent());

        Assertions.assertNull(personDao.findAll().firstResult());

        assertFalse(personDao.findAll().firstResultOptional().isPresent());

        Person person = makeSavedPersonDao();
        Assertions.assertNotNull(person.id);

        Assertions.assertEquals(1, personDao.count());
        Assertions.assertEquals(1, personDao.count("name = ?1", "stef"));
        Assertions.assertEquals(1, personDao.count("name = :name", Parameters.with("name", "stef").map()));
        Assertions.assertEquals(1, personDao.count("name = :name", Parameters.with("name", "stef")));
        Assertions.assertEquals(1, personDao.count("name", "stef"));

        Assertions.assertEquals(1, dogDao.count());
        Assertions.assertEquals(1, person.dogs.size());

        persons = personDao.findAll().list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = personDao.listAll();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        personStream = personDao.findAll().stream();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        personStream = personDao.streamAll();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        Assertions.assertEquals(person, personDao.findAll().firstResult());
        Assertions.assertEquals(person, personDao.findAll().singleResult());
        Assertions.assertEquals(person, personDao.findAll().singleResultOptional().get());

        persons = personDao.find("name = ?1", "stef").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        // full form
        persons = personDao.find("FROM Person2 WHERE name = ?1", "stef").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = personDao.find("name = ?1", "stef").withLock(LockModeType.PESSIMISTIC_READ).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = personDao.list("name = ?1", "stef");
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = personDao.find("name = :name", Parameters.with("name", "stef").map()).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = personDao.find("name = :name", Parameters.with("name", "stef")).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = personDao.list("name = :name", Parameters.with("name", "stef").map());
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = personDao.list("name = :name", Parameters.with("name", "stef"));
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = personDao.find("name", "stef").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        personStream = personDao.find("name = ?1", "stef").stream();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        personStream = personDao.stream("name = ?1", "stef");
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        personStream = personDao.stream("name = :name", Parameters.with("name", "stef").map());
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        personStream = personDao.stream("name = :name", Parameters.with("name", "stef"));
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        personStream = personDao.find("name", "stef").stream();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        Assertions.assertEquals(person, personDao.find("name", "stef").firstResult());
        Assertions.assertEquals(person, personDao.find("name", "stef").singleResult());
        Assertions.assertEquals(person, personDao.find("name", "stef").singleResultOptional().get());

        // named query
        persons = personDao.find("#Person.getByName", Parameters.with("name", "stef")).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));
        Assertions.assertThrows(PanacheQueryException.class, () -> personDao.find("#Person.namedQueryNotFound").list());
        namedQueryRepository.find("#NamedQueryMappedSuperClass.getAll").list();
        namedQueryRepository.find("#NamedQueryEntity.getAll").list();
        namedQueryWith2QueriesRepository.find("#NamedQueryWith2QueriesEntity.getAll1").list();
        namedQueryWith2QueriesRepository.find("#NamedQueryWith2QueriesEntity.getAll2").list();

        //empty query
        persons = personDao.find("").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));
        persons = personDao.find(null).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        Person byId = personDao.findById(person.id);
        Assertions.assertEquals(person, byId);

        byId = personDao.findByIdOptional(person.id).get();
        Assertions.assertEquals(person, byId);

        byId = personDao.findById(person.id, LockModeType.PESSIMISTIC_READ);
        Assertions.assertEquals(person, byId);

        byId = personDao.findByIdOptional(person.id, LockModeType.PESSIMISTIC_READ).get();
        Assertions.assertEquals(person, byId);

        personDao.delete(person);
        Assertions.assertEquals(0, personDao.count());

        person = makeSavedPersonDao();
        Assertions.assertEquals(1, personDao.count());
        Assertions.assertEquals(0, personDao.delete("name = ?1", "emmanuel"));
        Assertions.assertEquals(1, dogDao.delete("owner = ?1", person));
        Assertions.assertEquals(1, personDao.delete("name", "stef"));
        person = makeSavedPerson();
        Assertions.assertEquals(1, dogDao.delete("owner = :owner", Parameters.with("owner", person).map()));
        Assertions.assertEquals(1, personDao.delete("name", "stef"));
        person = makeSavedPerson();
        Assertions.assertEquals(1, dogDao.delete("owner = :owner", Parameters.with("owner", person)));
        Assertions.assertEquals(1, personDao.delete("name", "stef"));
        // full form
        person = makeSavedPerson();
        Assertions.assertEquals(1, dogDao.delete("FROM Dog WHERE owner = :owner", Parameters.with("owner", person)));
        Assertions.assertEquals(1, personDao.delete("FROM Person2 WHERE name = ?1", "stef"));
        person = makeSavedPerson();
        Assertions.assertEquals(1, dogDao.delete("DELETE FROM Dog WHERE owner = :owner", Parameters.with("owner", person)));
        Assertions.assertEquals(1, personDao.delete("DELETE FROM Person2 WHERE name = ?1", "stef"));

        Assertions.assertEquals(0, personDao.deleteAll());

        makeSavedPersonDao();
        Assertions.assertEquals(1, dogDao.deleteAll());
        Assertions.assertEquals(1, personDao.deleteAll());

        testPersistDao(PersistTest.Iterable);
        testPersistDao(PersistTest.Stream);
        testPersistDao(PersistTest.Variadic);
        Assertions.assertEquals(6, personDao.deleteAll());

        testSortingDao();

        // paging
        for (int i = 0; i < 7; i++) {
            makeSavedPersonDao(String.valueOf(i));
        }
        testPaging(personDao.findAll());
        testPaging(personDao.find("ORDER BY name"));

        //range
        testRange(personDao.findAll());
        testRange(personDao.find("ORDER BY name"));

        try {
            personDao.findAll().singleResult();
            Assertions.fail("singleResult should have thrown");
        } catch (NonUniqueResultException x) {
        }

        Assertions.assertNotNull(personDao.findAll().firstResult());

        Assertions.assertEquals(7, personDao.deleteAll());

        testUpdateDAO();

        //delete by id
        Person toRemove = new Person();
        toRemove.name = "testDeleteById";
        toRemove.uniqueName = "testDeleteByIdUnique";
        personDao.persist(toRemove);
        assertTrue(personDao.deleteById(toRemove.id));
        personDao.deleteById(666L);//not existing

        //flush
        Person person1 = new Person();
        person1.name = "testFlush1";
        person1.uniqueName = "unique";
        personDao.persist(person1);
        Person person2 = new Person();
        person2.name = "testFlush2";
        person2.uniqueName = "unique";
        try {
            personDao.persistAndFlush(person2);
            Assertions.fail();
        } catch (PersistenceException pe) {
            //this is expected
        }

        return "OK";
    }

    private void testSortingDao() {
        Person person1 = new Person();
        person1.name = "stef";
        person1.status = Status.LIVING;
        personDao.persist(person1);

        Person person2 = new Person();
        person2.name = "stef";
        person2.status = Status.DECEASED;
        personDao.persist(person2);

        Person person3 = new Person();
        person3.name = "emmanuel";
        person3.status = Status.LIVING;
        personDao.persist(person3);

        Sort sort1 = Sort.by("name", "status");
        List<Person> order1 = Arrays.asList(person3, person2, person1);

        List<Person> list = personDao.findAll(sort1).list();
        Assertions.assertEquals(order1, list);

        list = personDao.listAll(sort1);
        Assertions.assertEquals(order1, list);

        list = personDao.streamAll(sort1).collect(Collectors.toList());
        Assertions.assertEquals(order1, list);

        Sort sort2 = Sort.descending("name", "status");
        List<Person> order2 = Arrays.asList(person1, person2);

        list = personDao.find("name", sort2, "stef").list();
        Assertions.assertEquals(order2, list);

        list = personDao.list("name", sort2, "stef");
        Assertions.assertEquals(order2, list);

        list = personDao.stream("name", sort2, "stef").collect(Collectors.toList());
        Assertions.assertEquals(order2, list);

        list = personDao.find("name = :name", sort2, Parameters.with("name", "stef").map()).list();
        Assertions.assertEquals(order2, list);

        list = personDao.list("name = :name", sort2, Parameters.with("name", "stef").map());
        Assertions.assertEquals(order2, list);

        list = personDao.stream("name = :name", sort2, Parameters.with("name", "stef").map()).collect(Collectors.toList());
        Assertions.assertEquals(order2, list);

        list = personDao.find("name = :name", sort2, Parameters.with("name", "stef")).list();
        Assertions.assertEquals(order2, list);

        list = personDao.list("name = :name", sort2, Parameters.with("name", "stef"));
        Assertions.assertEquals(order2, list);

        list = personDao.stream("name = :name", sort2, Parameters.with("name", "stef")).collect(Collectors.toList());
        Assertions.assertEquals(order2, list);

        Assertions.assertEquals(3, Person.deleteAll());
    }

    enum PersistTest {
        Iterable,
        Variadic,
        Stream;
    }

    private void testPersistDao(PersistTest persistTest) {
        Person person1 = new Person();
        person1.name = "stef1";
        Person person2 = new Person();
        person2.name = "stef2";
        assertFalse(person1.isPersistent());
        assertFalse(person2.isPersistent());
        switch (persistTest) {
            case Iterable:
                personDao.persist(Arrays.asList(person1, person2));
                break;
            case Stream:
                personDao.persist(Stream.of(person1, person2));
                break;
            case Variadic:
                personDao.persist(person1, person2);
                break;
        }
        assertTrue(person1.isPersistent());
        assertTrue(person2.isPersistent());
    }

    private Person makeSavedPersonDao(String suffix) {
        Person person = new Person();
        person.name = "stef" + suffix;
        person.status = Status.LIVING;
        person.address = new Address("stef street");
        addressDao.persist(person.address);

        personDao.persist(person);

        return person;
    }

    private Person makeSavedPersonDao() {
        Person person = makeSavedPersonDao("");

        Dog dog = new Dog("octave", "dalmatian");
        dog.owner = person;
        person.dogs.add(dog);
        dogDao.persist(dog);

        return person;
    }

    private void testPaging(PanacheQuery<Person> query) {
        // No paging allowed until a page is setup
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.firstPage(),
                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.previousPage(),
                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.nextPage(),
                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.lastPage(),
                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.hasNextPage(),
                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.hasPreviousPage(),
                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.page(),
                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.pageCount(),
                "UnsupportedOperationException should have thrown");

        // ints
        List<Person> persons = query.page(0, 3).list();
        Assertions.assertEquals(3, persons.size());
        Assertions.assertEquals("stef0", persons.get(0).name);
        Assertions.assertEquals("stef1", persons.get(1).name);
        Assertions.assertEquals("stef2", persons.get(2).name);

        persons = query.page(1, 3).list();
        Assertions.assertEquals(3, persons.size());
        Assertions.assertEquals("stef3", persons.get(0).name);
        Assertions.assertEquals("stef4", persons.get(1).name);
        Assertions.assertEquals("stef5", persons.get(2).name);

        persons = query.page(2, 3).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals("stef6", persons.get(0).name);

        persons = query.page(2, 4).list();
        Assertions.assertEquals(0, persons.size());

        // page
        Page page = new Page(3);
        persons = query.page(page).list();
        Assertions.assertEquals(3, persons.size());
        Assertions.assertEquals("stef0", persons.get(0).name);
        Assertions.assertEquals("stef1", persons.get(1).name);
        Assertions.assertEquals("stef2", persons.get(2).name);

        page = page.next();
        persons = query.page(page).list();
        Assertions.assertEquals(3, persons.size());
        Assertions.assertEquals("stef3", persons.get(0).name);
        Assertions.assertEquals("stef4", persons.get(1).name);
        Assertions.assertEquals("stef5", persons.get(2).name);

        page = page.next();
        persons = query.page(page).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals("stef6", persons.get(0).name);

        page = page.next();
        persons = query.page(page).list();
        Assertions.assertEquals(0, persons.size());

        // query paging
        page = new Page(3);
        persons = query.page(page).list();
        Assertions.assertEquals(3, persons.size());
        Assertions.assertEquals("stef0", persons.get(0).name);
        Assertions.assertEquals("stef1", persons.get(1).name);
        Assertions.assertEquals("stef2", persons.get(2).name);
        assertTrue(query.hasNextPage());
        assertFalse(query.hasPreviousPage());

        persons = query.nextPage().list();
        Assertions.assertEquals(1, query.page().index);
        Assertions.assertEquals(3, query.page().size);
        Assertions.assertEquals(3, persons.size());
        Assertions.assertEquals("stef3", persons.get(0).name);
        Assertions.assertEquals("stef4", persons.get(1).name);
        Assertions.assertEquals("stef5", persons.get(2).name);
        assertTrue(query.hasNextPage());
        assertTrue(query.hasPreviousPage());

        persons = query.nextPage().list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals("stef6", persons.get(0).name);
        assertFalse(query.hasNextPage());
        assertTrue(query.hasPreviousPage());

        persons = query.nextPage().list();
        Assertions.assertEquals(0, persons.size());

        Assertions.assertEquals(7, query.count());
        Assertions.assertEquals(3, query.pageCount());

        // mix page with range
        persons = query.page(0, 3).range(0, 1).list();
        Assertions.assertEquals(2, persons.size());
        Assertions.assertEquals("stef0", persons.get(0).name);
        Assertions.assertEquals("stef1", persons.get(1).name);
    }

    private void testRange(PanacheQuery<Person> query) {
        List<Person> persons = query.range(0, 2).list();
        Assertions.assertEquals(3, persons.size());
        Assertions.assertEquals("stef0", persons.get(0).name);
        Assertions.assertEquals("stef1", persons.get(1).name);
        Assertions.assertEquals("stef2", persons.get(2).name);

        persons = query.range(3, 5).list();
        Assertions.assertEquals(3, persons.size());
        Assertions.assertEquals("stef3", persons.get(0).name);
        Assertions.assertEquals("stef4", persons.get(1).name);
        Assertions.assertEquals("stef5", persons.get(2).name);

        persons = query.range(6, 8).list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals("stef6", persons.get(0).name);

        persons = query.range(8, 12).list();
        Assertions.assertEquals(0, persons.size());

        // mix range with page
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).nextPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).previousPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).pageCount());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).lastPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).firstPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).hasPreviousPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).hasNextPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).page());
        // this is valid as we switch from range to page
        persons = query.range(0, 2).page(0, 3).list();
        Assertions.assertEquals(3, persons.size());
        Assertions.assertEquals("stef0", persons.get(0).name);
        Assertions.assertEquals("stef1", persons.get(1).name);
        Assertions.assertEquals("stef2", persons.get(2).name);
    }

    @GET
    @Path("accessors")
    public String testAccessors() throws NoSuchMethodException, SecurityException {
        checkMethod(AccessorEntity.class, "getString", String.class);
        checkMethod(AccessorEntity.class, "isBool", boolean.class);
        checkMethod(AccessorEntity.class, "getC", char.class);
        checkMethod(AccessorEntity.class, "getS", short.class);
        checkMethod(AccessorEntity.class, "getI", int.class);
        checkMethod(AccessorEntity.class, "getL", long.class);
        checkMethod(AccessorEntity.class, "getF", float.class);
        checkMethod(AccessorEntity.class, "getD", double.class);
        checkMethod(AccessorEntity.class, "getT", Object.class);
        checkMethod(AccessorEntity.class, "getT2", Object.class);

        checkMethod(AccessorEntity.class, "setString", void.class, String.class);
        checkMethod(AccessorEntity.class, "setBool", void.class, boolean.class);
        checkMethod(AccessorEntity.class, "setC", void.class, char.class);
        checkMethod(AccessorEntity.class, "setS", void.class, short.class);
        checkMethod(AccessorEntity.class, "setI", void.class, int.class);
        checkMethod(AccessorEntity.class, "setL", void.class, long.class);
        checkMethod(AccessorEntity.class, "setF", void.class, float.class);
        checkMethod(AccessorEntity.class, "setD", void.class, double.class);
        checkMethod(AccessorEntity.class, "setT", void.class, Object.class);
        checkMethod(AccessorEntity.class, "setT2", void.class, Object.class);

        try {
            checkMethod(AccessorEntity.class, "getTrans2", Object.class);
            Assertions.fail("transient field should have no getter: trans2");
        } catch (NoSuchMethodException x) {
        }

        try {
            checkMethod(AccessorEntity.class, "setTrans2", void.class, Object.class);
            Assertions.fail("transient field should have no setter: trans2");
        } catch (NoSuchMethodException x) {
        }

        // Now check that accessors are called
        AccessorEntity entity = new AccessorEntity();
        @SuppressWarnings("unused")
        byte b = entity.b;
        Assertions.assertEquals(1, entity.getBCalls);
        entity.i = 2;
        Assertions.assertEquals(1, entity.setICalls);
        Object trans = entity.trans;
        Assertions.assertEquals(0, entity.getTransCalls);
        entity.trans = trans;
        Assertions.assertEquals(0, entity.setTransCalls);

        // accessors inside the entity itself
        entity.method();
        Assertions.assertEquals(2, entity.getBCalls);
        Assertions.assertEquals(2, entity.setICalls);

        return "OK";
    }

    private void checkMethod(Class<?> klass, String name, Class<?> returnType, Class<?>... params)
            throws NoSuchMethodException, SecurityException {
        Method method = klass.getMethod(name, params);
        Assertions.assertEquals(returnType, method.getReturnType());
    }

    @GET
    @Path("model1")
    @Transactional
    public String testModel1() {
        Assertions.assertEquals(0, Person.count());

        Person person = makeSavedPerson("");
        SelfDirtinessTracker trackingPerson = (SelfDirtinessTracker) person;

        String[] dirtyAttributes = trackingPerson.$$_hibernate_getDirtyAttributes();
        Assertions.assertEquals(0, dirtyAttributes.length);

        person.name = "1";

        dirtyAttributes = trackingPerson.$$_hibernate_getDirtyAttributes();
        Assertions.assertEquals(1, dirtyAttributes.length);

        Assertions.assertEquals(1, Person.count());
        return "OK";
    }

    @GET
    @Path("model2")
    @Transactional
    public String testModel2() {
        Assertions.assertEquals(1, Person.count());

        Person person = Person.findAll().firstResult();
        Assertions.assertEquals("1", person.name);

        person.name = "2";
        return "OK";
    }

    @GET
    @Path("projection")
    @Transactional
    public String testProjection() {
        Assertions.assertEquals(1, Person.count());

        PersonName person = Person.findAll().project(PersonName.class).firstResult();
        Assertions.assertEquals("2", person.name);

        person = Person.find("name", "2").project(PersonName.class).firstResult();
        Assertions.assertEquals("2", person.name);

        person = Person.find("name = ?1", "2").project(PersonName.class).firstResult();
        Assertions.assertEquals("2", person.name);

        person = Person.find("name = :name", Parameters.with("name", "2")).project(PersonName.class).firstResult();
        Assertions.assertEquals("2", person.name);

        PanacheQuery<PersonName> query = Person.findAll().project(PersonName.class).page(0, 2);
        Assertions.assertEquals(1, query.list().size());
        query.nextPage();
        Assertions.assertEquals(0, query.list().size());

        Assertions.assertEquals(1, Person.findAll().project(PersonName.class).count());

        Person owner = makeSavedPerson();
        DogDto dogDto = Dog.findAll().project(DogDto.class).firstResult();
        Assertions.assertEquals("stef", dogDto.ownerName);
        owner.delete();

        return "OK";
    }

    @GET
    @Path("model3")
    @Transactional
    public String testModel3() {
        Assertions.assertEquals(1, Person.count());

        Person person = Person.findAll().firstResult();
        Assertions.assertEquals("2", person.name);

        Dog.deleteAll();
        Person.deleteAll();
        Address.deleteAll();
        Assertions.assertEquals(0, Person.count());

        return "OK";
    }

    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @GET
    @Path("ignored-properties")
    public Person ignoredProperties() throws NoSuchMethodException, SecurityException {
        Person.class.getMethod("$$_hibernate_read_id");
        Person.class.getMethod("$$_hibernate_read_name");
        try {
            Person.class.getMethod("$$_hibernate_read_persistent");
            Assertions.fail();
        } catch (NoSuchMethodException e) {
        }

        // no need to persist it, we can fake it
        Person person = new Person();
        person.id = 666l;
        person.name = "Eddie";
        person.status = Status.DECEASED;
        return person;
    }

    @Inject
    Bug5274EntityRepository bug5274EntityRepository;

    @GET
    @Path("5274")
    @Transactional
    public String testBug5274() {
        bug5274EntityRepository.count();
        return "OK";
    }

    @Inject
    Bug5885EntityRepository bug5885EntityRepository;

    @GET
    @Path("5885")
    @Transactional
    public String testBug5885() {
        bug5885EntityRepository.findById(1L);
        return "OK";
    }

    @GET
    @Path("testJaxbAnnotationTransfer")
    public String testJaxbAnnotationTransfer() throws Exception {
        // Test for fix to this bug: https://github.com/quarkusio/quarkus/issues/6021

        // Ensure that any JAX-B annotations are properly moved to generated getters
        Method m = JAXBEntity.class.getMethod("getNamedAnnotatedProp");
        XmlAttribute anno = m.getAnnotation(XmlAttribute.class);
        assertNotNull(anno);
        assertEquals("Named", anno.name());
        assertNull(m.getAnnotation(XmlTransient.class));

        m = JAXBEntity.class.getMethod("getDefaultAnnotatedProp");
        anno = m.getAnnotation(XmlAttribute.class);
        assertNotNull(anno);
        assertEquals("##default", anno.name());
        assertNull(m.getAnnotation(XmlTransient.class));

        m = JAXBEntity.class.getMethod("getUnAnnotatedProp");
        assertNull(m.getAnnotation(XmlAttribute.class));
        assertNull(m.getAnnotation(XmlTransient.class));

        m = JAXBEntity.class.getMethod("getTransientProp");
        assertNull(m.getAnnotation(XmlAttribute.class));
        assertNotNull(m.getAnnotation(XmlTransient.class));

        m = JAXBEntity.class.getMethod("getArrayAnnotatedProp");
        assertNull(m.getAnnotation(XmlTransient.class));
        XmlElements elementsAnno = m.getAnnotation(XmlElements.class);
        assertNotNull(elementsAnno);
        assertNotNull(elementsAnno.value());
        assertEquals(2, elementsAnno.value().length);
        assertEquals("array1", elementsAnno.value()[0].name());
        assertEquals("array2", elementsAnno.value()[1].name());

        // Ensure that all original fields were labeled @XmlTransient and had their original JAX-B annotations removed
        ensureFieldSanitized("namedAnnotatedProp");
        ensureFieldSanitized("transientProp");
        ensureFieldSanitized("defaultAnnotatedProp");
        ensureFieldSanitized("unAnnotatedProp");
        ensureFieldSanitized("arrayAnnotatedProp");

        return "OK";
    }

    private void ensureFieldSanitized(String fieldName) throws Exception {
        Field f = JAXBEntity.class.getDeclaredField(fieldName);
        assertNull(f.getAnnotation(XmlAttribute.class));
        assertNotNull(f.getAnnotation(XmlTransient.class));
    }

    @GET
    @Path("composite")
    @Transactional
    public String testCompositeKey() {
        ObjectWithCompositeId obj = new ObjectWithCompositeId();
        obj.part1 = "part1";
        obj.part2 = "part2";
        obj.description = "description";
        obj.persist();

        ObjectWithCompositeId.ObjectKey key = new ObjectWithCompositeId.ObjectKey("part1", "part2");
        ObjectWithCompositeId result = ObjectWithCompositeId.findById(key);
        assertNotNull(result);

        boolean deleted = ObjectWithCompositeId.deleteById(key);
        assertTrue(deleted);

        ObjectWithCompositeId.ObjectKey notExistingKey = new ObjectWithCompositeId.ObjectKey("notexist1", "notexist2");
        deleted = ObjectWithCompositeId.deleteById(key);
        assertFalse(deleted);

        ObjectWithEmbeddableId.ObjectKey embeddedKey = new ObjectWithEmbeddableId.ObjectKey("part1", "part2");
        ObjectWithEmbeddableId embeddable = new ObjectWithEmbeddableId();
        embeddable.key = embeddedKey;
        embeddable.description = "description";
        embeddable.persist();

        ObjectWithEmbeddableId embeddableResult = ObjectWithEmbeddableId.findById(embeddedKey);
        assertNotNull(embeddableResult);

        deleted = ObjectWithEmbeddableId.deleteById(embeddedKey);
        assertTrue(deleted);

        ObjectWithEmbeddableId.ObjectKey notExistingEmbeddedKey = new ObjectWithEmbeddableId.ObjectKey("notexist1",
                "notexist2");
        deleted = ObjectWithEmbeddableId.deleteById(embeddedKey);
        assertFalse(deleted);

        return "OK";
    }

    @GET
    @Path("7721")
    @Transactional
    public String testBug7721() {
        Bug7721Entity entity = new Bug7721Entity();
        entity.persist();
        entity.delete();
        return "OK";
    }

    @GET
    @Path("8254")
    @Transactional
    public String testBug8254() {
        CatOwner owner = new CatOwner("8254");
        owner.persist();
        new Cat(owner).persist();
        new Cat(owner).persist();
        new Cat(owner).persist();

        // This used to fail with an invalid query "SELECT COUNT(*) SELECT DISTINCT cat.owner FROM Cat cat WHERE cat.owner = ?1"
        // Should now result in a valid query "SELECT COUNT(DISTINCT cat.owner) FROM Cat cat WHERE cat.owner = ?1"
        assertEquals(1L, CatOwner.find("SELECT DISTINCT cat.owner FROM Cat cat WHERE cat.owner = ?1", owner).count());

        // This used to fail with an invalid query "SELECT COUNT(*) SELECT cat.owner FROM Cat cat WHERE cat.owner = ?1"
        // Should now result in a valid query "SELECT COUNT(cat.owner) FROM Cat cat WHERE cat.owner = ?1"
        assertEquals(3L, CatOwner.find("SELECT cat.owner FROM Cat cat WHERE cat.owner = ?1", owner).count());

        // This used to fail with an invalid query "SELECT COUNT(*) SELECT cat FROM Cat cat WHERE cat.owner = ?1"
        // Should now result in a valid query "SELECT COUNT(cat) FROM Cat cat WHERE cat.owner = ?1"
        assertEquals(3L, Cat.find("SELECT cat FROM Cat cat WHERE cat.owner = ?1", owner).count());

        // This didn't use to fail. Make sure it still doesn't.
        assertEquals(3L, Cat.find("FROM Cat WHERE owner = ?1", owner).count());
        assertEquals(3L, Cat.find("owner", owner).count());
        assertEquals(1L, CatOwner.find("name = ?1", "8254").count());

        return "OK";
    }

    @GET
    @Path("9025")
    @Transactional
    public String testBug9025() {
        Fruit apple = new Fruit("apple", "red");
        Fruit orange = new Fruit("orange", "orange");
        Fruit banana = new Fruit("banana", "yellow");

        Fruit.persist(apple, orange, banana);

        PanacheQuery<Fruit> query = Fruit.find(
                "select name, color from Fruit").page(Page.ofSize(1));

        List<Fruit> results = query.list();

        int pageCount = query.pageCount();

        return "OK";
    }

    @GET
    @Path("9036")
    @Transactional
    public String testBug9036() {
        Person.deleteAll();

        Person emptyPerson = new Person();
        emptyPerson.persist();

        Person deadPerson = new Person();
        deadPerson.name = "Stef";
        deadPerson.status = Status.DECEASED;
        deadPerson.persist();

        Person livePerson = new Person();
        livePerson.name = "Stef";
        livePerson.status = Status.LIVING;
        livePerson.persist();

        assertEquals(3, Person.count());
        assertEquals(3, Person.listAll().size());

        // should be filtered
        PanacheQuery<Person> query = Person.findAll(Sort.by("id")).filter("Person.isAlive").filter("Person.hasName",
                Parameters.with("name", "Stef"));
        assertEquals(1, query.count());
        assertEquals(1, query.list().size());
        assertEquals(livePerson, query.list().get(0));
        assertEquals(1, query.stream().count());
        assertEquals(livePerson, query.firstResult());
        assertEquals(livePerson, query.singleResult());

        // these should be unaffected
        assertEquals(3, Person.count());
        assertEquals(3, Person.listAll().size());

        Person.deleteAll();

        return "OK";
    }

    @GET
    @Path("testFilterWithCollections")
    @Transactional
    public String testFilterWithCollections() {
        Person.deleteAll();

        Person stefPerson = new Person();
        stefPerson.name = "Stef";
        stefPerson.persist();

        Person josePerson = new Person();
        josePerson.name = "Jose";
        josePerson.persist();

        Person victorPerson = new Person();
        victorPerson.name = "Victor";
        victorPerson.persist();

        assertEquals(3, Person.count());

        List<String> namesParameter = Arrays.asList("Jose", "Victor");

        // Try with different collection types
        List<Object> collectionsValues = Arrays.asList(
                // Using directly a list:
                namesParameter,
                // Using another collection,
                new HashSet<>(namesParameter),
                // Using array
                namesParameter.toArray(new String[namesParameter.size()]));

        for (Object collectionValue : collectionsValues) {
            // should be filtered
            List<Person> found = Person.findAll(Sort.by("id")).filter("Person.name.in",
                    Parameters.with("names", collectionValue))
                    .list();
            assertEquals(2, found.size(),
                    "Expected 2 entries when using parameter " + collectionValue.getClass());
            assertTrue(found.stream().anyMatch(p -> p.name.contains("Jose")),
                    "Jose was not found when using parameter " + collectionValue.getClass());
            assertTrue(found.stream().anyMatch(p -> p.name.contains("Victor")),
                    "Victor was not found when using parameter " + collectionValue.getClass());
        }

        Person.deleteAll();

        return "OK";
    }
}
