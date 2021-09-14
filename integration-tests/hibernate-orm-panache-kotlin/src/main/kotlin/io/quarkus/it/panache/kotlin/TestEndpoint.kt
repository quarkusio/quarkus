package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.quarkus.panache.common.exception.PanacheQueryException
import org.hibernate.engine.spi.SelfDirtinessTracker
import org.hibernate.jpa.QueryHints
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import java.lang.UnsupportedOperationException
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.UUID
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.inject.Inject
import javax.persistence.LockModeType
import javax.persistence.NoResultException
import javax.persistence.NonUniqueResultException
import javax.persistence.PersistenceException
import javax.transaction.Transactional
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElements
import javax.xml.bind.annotation.XmlTransient

/**
 * Various tests covering Panache functionality. All tests should work in both standard JVM and in native mode.
 */
@Path("test")
class TestEndpoint {
    @GET
    @Path("model")
    @Transactional
    fun testModel(): String {
        Person.flush()
        Assertions.assertNotNull(Person.getEntityManager());

        Assertions.assertDoesNotThrow {
            Person.findById(Long.MIN_VALUE)
        }
        Assertions.assertDoesNotThrow {
            Person.find("name = ?1", UUID.randomUUID().toString()).firstResult()
        }
        Assertions.assertThrows(NoResultException::class.java, {
            Person.find("name = ?1", UUID.randomUUID().toString()).singleResult()
        })

        var persons: List<Person> = Person.findAll().list()
        Assertions.assertEquals(0, persons.size)

        persons = Person.listAll()
        Assertions.assertEquals(0, persons.size)

        var personStream = Person.findAll().stream()
        Assertions.assertEquals(0, personStream.count())

        personStream = Person.streamAll()
        Assertions.assertEquals(0, personStream.count())
        try {
            Person.findAll().singleResult()
            Assertions.fail<Any>("singleResult should have thrown")
        } catch (x: NoResultException) {
        }

        Assertions.assertNull(Person.findAll().firstResult())

        var person: Person = makeSavedPerson()
        Assertions.assertNotNull(person.id)

        Assertions.assertEquals(1, Person.count())
        Assertions.assertEquals(1, Person.count("name = ?1", "stef"))
        Assertions.assertEquals(1, Person.count("name = :name", Parameters.with("name", "stef").map()))
        Assertions.assertEquals(1, Person.count("name = :name", Parameters.with("name", "stef")))
        Assertions.assertEquals(1, Person.count("name", "stef"))

        Assertions.assertEquals(1, Dog.count())
        Assertions.assertEquals(1, person.dogs.size)

        persons = Person.findAll().list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = Person.listAll()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        personStream = Person.findAll().stream()
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        personStream = Person.streamAll()
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        Assertions.assertEquals(person, Person.findAll().firstResult())
        Assertions.assertEquals(person, Person.findAll().singleResult())

        persons = Person.find("name = ?1", "stef").list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = Person.find("name = ?1", "stef").withLock(LockModeType.PESSIMISTIC_READ).list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        // next calls to this query will be cached
        persons = Person.find("name = ?1", "stef").withHint(QueryHints.HINT_CACHEABLE, "true").list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = Person.list("name = ?1", "stef")
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = Person.find("name = :name", Parameters.with("name", "stef").map()).list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = Person.find("name = :name", Parameters.with("name", "stef")).list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = Person.list("name = :name", Parameters.with("name", "stef").map())
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = Person.list("name = :name", Parameters.with("name", "stef"))
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = Person.find("name", "stef").list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        personStream = Person.find("name = ?1", "stef").stream()
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        personStream = Person.stream("name = ?1", "stef")
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        personStream = Person.stream("name = :name", Parameters.with("name", "stef").map())
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        personStream = Person.stream("name = :name", Parameters.with("name", "stef"))
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        personStream = Person.find("name", "stef").stream()
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))
        Assertions.assertEquals(person, Person.find("name", "stef").firstResult())
        Assertions.assertEquals(person, Person.find("name", "stef").singleResult())

        var byId: Person? = person.id?.let { Person.findById(it) }
        Assertions.assertEquals(person, byId)
        Assertions.assertEquals("Person(id=${person.id}, name=${person.name}, status=${person.status})", byId.toString())

        byId = person.id?.let { Person.findById(it, LockModeType.PESSIMISTIC_READ) }
        Assertions.assertEquals(person, byId)
        Assertions.assertEquals("Person(id=${person.id}, name=${person.name}, status=${person.status})", byId.toString())
        Assertions.assertNotNull(person.dogs.toString())

        person.delete()
        Assertions.assertEquals(0, Person.count())

        person = makeSavedPerson()
        Assertions.assertEquals(1, Person.count())
        Assertions.assertEquals(0, Person.delete("name = ?1", "emmanuel"))
        Assertions.assertEquals(1, Dog.delete("owner = ?1", person))
        Assertions.assertEquals(1, Person.delete("name", "stef"))
        person = makeSavedPerson()
        Assertions.assertEquals(1, Dog.delete("owner = :owner", Parameters.with("owner", person).map()))
        Assertions.assertEquals(1, Person.delete("name", "stef"))
        person = makeSavedPerson()
        Assertions.assertEquals(1, Dog.delete("owner = :owner", Parameters.with("owner", person)))
        Assertions.assertEquals(1, Person.delete("name", "stef"))

        Assertions.assertEquals(0, Person.deleteAll())

        makeSavedPerson()
        Assertions.assertEquals(1, Dog.deleteAll())
        Assertions.assertEquals(1, Person.deleteAll())

        testPersist(PersistTest.Iterable)
        testPersist(PersistTest.Stream)
        testPersist(PersistTest.Variadic)
        Assertions.assertEquals(6, Person.deleteAll())

        testSorting()

        // paging
        for (i in 0..6) {
            makeSavedPerson(i.toString())
        }
        testPaging(Person.findAll())
        testPaging(Person.find("ORDER BY name"))

        try {
            Person.findAll().singleResult()
            Assertions.fail<Any>("singleResult should have thrown")
        } catch (x: NonUniqueResultException) {
        }

        Assertions.assertNotNull(Person.findAll().firstResult())

        Assertions.assertEquals(7, Person.deleteAll())

        testUpdate()

        // persistAndFlush
        val person1 = Person()
        person1.name = "testFLush1"
        person1.uniqueName = "unique"
        person1.persist()
        val person2 = Person()
        person2.name = "testFLush2"
        person2.uniqueName = "unique"
        try {
            person2.persistAndFlush()
            Assertions.fail<Any>()
        } catch (pe: PersistenceException) {
            //this is expected
        }
        return "OK"
    }

    private fun testUpdate() {
        makeSavedPerson("p1")
        makeSavedPerson("p2")

        var updateByIndexParameter: Int = Person.update("update from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1")
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated")

        var updateByNamedParameter: Int = Person.update("update from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                Parameters.with("pName", "stefp2").map())
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated")

        Assertions.assertEquals(2, Person.deleteAll())

        makeSavedPerson("p1")
        makeSavedPerson("p2")

        updateByIndexParameter = Person.update("from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1")
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated")

        updateByNamedParameter = Person.update("from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                Parameters.with("pName", "stefp2").map())
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated")

        Assertions.assertEquals(2, Person.deleteAll())

        makeSavedPerson("p1")
        makeSavedPerson("p2")

        updateByIndexParameter = Person.update("set name = 'stefNEW' where name = ?1", "stefp1")
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated")

        updateByNamedParameter = Person.update("set name = 'stefNEW' where name = :pName",
                Parameters.with("pName", "stefp2").map())
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated")

        Assertions.assertEquals(2, Person.deleteAll())

        makeSavedPerson("p1")
        makeSavedPerson("p2")

        updateByIndexParameter = Person.update("name = 'stefNEW' where name = ?1", "stefp1")
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated")

        updateByNamedParameter = Person.update("name = 'stefNEW' where name = :pName",
                Parameters.with("pName", "stefp2").map())
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated")

        Assertions.assertEquals(2, Person.deleteAll())

        makeSavedPerson("p1")
        makeSavedPerson("p2")

        updateByIndexParameter = Person.update("name = 'stefNEW' where name = ?1", "stefp1")
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated")

        updateByNamedParameter = Person.update("name = 'stefNEW' where name = :pName",
                Parameters.with("pName", "stefp2"))
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated")

        Assertions.assertEquals(2, Person.deleteAll())

//        Assertions.assertThrows(PanacheQueryException::class.java, { Person.update(null) },
//                "PanacheQueryException should have thrown")

        Assertions.assertThrows(PanacheQueryException::class.java, { Person.update(" ") },
                "PanacheQueryException should have thrown")
    }

    private fun testUpdateDAO() {
        makeSavedPerson("p1")
        makeSavedPerson("p2")

        var updateByIndexParameter: Int = personRepository.update("update from Person2 p set p.name = 'stefNEW' where p.name = ?1",
                "stefp1")
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated")

        var updateByNamedParameter: Int = personRepository.update("update from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                Parameters.with("pName", "stefp2").map())
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated")

        Assertions.assertEquals(2, personRepository.deleteAll())

        makeSavedPerson("p1")
        makeSavedPerson("p2")

        updateByIndexParameter = personRepository.update("from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1")
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated")

        updateByNamedParameter = personRepository.update("from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                Parameters.with("pName", "stefp2").map())
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated")

        Assertions.assertEquals(2, personRepository.deleteAll())

        makeSavedPerson("p1")
        makeSavedPerson("p2")

        updateByIndexParameter = personRepository.update("set name = 'stefNEW' where name = ?1", "stefp1")
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated")

        updateByNamedParameter = personRepository.update("set name = 'stefNEW' where name = :pName",
                Parameters.with("pName", "stefp2").map())
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated")

        Assertions.assertEquals(2, personRepository.deleteAll())

        makeSavedPerson("p1")
        makeSavedPerson("p2")

        updateByIndexParameter = personRepository.update("name = 'stefNEW' where name = ?1", "stefp1")
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated")

        updateByNamedParameter = personRepository.update("name = 'stefNEW' where name = :pName",
                Parameters.with("pName", "stefp2").map())
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated")

        Assertions.assertEquals(2, personRepository.deleteAll())

        makeSavedPerson("p1")
        makeSavedPerson("p2")

        updateByIndexParameter = personRepository.update("name = 'stefNEW' where name = ?1", "stefp1")
        Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated")

        updateByNamedParameter = personRepository.update("name = 'stefNEW' where name = :pName",
                Parameters.with("pName", "stefp2"))
        Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated")

        Assertions.assertEquals(2, personRepository.deleteAll())

//        Assertions.assertThrows(PanacheQueryException::class.java, { personDao.update(null) },
//                "PanacheQueryException should have thrown")

        Assertions.assertThrows(PanacheQueryException::class.java, { personRepository.update(" ") },
                "PanacheQueryException should have thrown")
    }

    private fun testSorting() {
        val person1 = Person()
        person1.name = "stef"
        person1.status = Status.LIVING
        person1.persist()

        val person2 = Person()
        person2.name = "stef"
        person2.status = Status.DECEASED
        person2.persist()

        val person3 = Person()
        person3.name = "emmanuel"
        person3.status = Status.LIVING
        person3.persist()

        val sort1 = Sort.by("name", "status")
        val order1: List<Person> = listOf(person3, person2, person1)

        var list: List<Person?> = Person.findAll(sort1).list()
        Assertions.assertEquals(order1, list)

        list = Person.listAll(sort1)
        Assertions.assertEquals(order1, list)

        list = Person.streamAll(sort1).collect(Collectors.toList())
        Assertions.assertEquals(order1, list)

        val sort2 = Sort.descending("name", "status")
        val order2: List<Person> = listOf(person1, person2)

        list = Person.find("name", sort2, "stef").list()
        Assertions.assertEquals(order2, list)

        list = Person.list("name", sort2, "stef")

        Assertions.assertEquals(order2, list)
        list = Person.stream("name", sort2, "stef").collect(Collectors.toList())
        Assertions.assertEquals(order2, list)

        list = Person.find("name = :name", sort2, Parameters.with("name", "stef").map()).list()
        Assertions.assertEquals(order2, list)

        list = Person.list("name = :name", sort2, Parameters.with("name", "stef").map())
        Assertions.assertEquals(order2, list)

        list = Person.stream("name = :name", sort2, Parameters.with("name", "stef").map())
                .collect(Collectors.toList())
        Assertions.assertEquals(order2, list)

        list = Person.find("name = :name", sort2, Parameters.with("name", "stef")).list()
        Assertions.assertEquals(order2, list)

        list = Person.list("name = :name", sort2, Parameters.with("name", "stef"))
        Assertions.assertEquals(order2, list)

        list = Person.stream("name = :name", sort2, Parameters.with("name", "stef")).collect(Collectors.toList())
        Assertions.assertEquals(order2, list)

        Assertions.assertEquals(3, Person.deleteAll())
    }

    private fun makeSavedPerson(suffix: String): Person {
        val person = Person()
        person.name = "stef$suffix"
        person.status = Status.LIVING
        person.address = Address("stef street")
        person.address?.persist()

        person.persist()
        return person
    }

    private fun makeSavedPerson(): Person {
        val person: Person = makeSavedPerson("")

        val dog = Dog("octave", "dalmatian")
        dog.owner = person
        person.dogs.add(dog)
        dog.persist()

        return person
    }

    private fun testPersist(persistTest: PersistTest) {
        val person1 = Person()
        person1.name = "stef1"
        val person2 = Person()
        person2.name = "stef2"
        Assertions.assertFalse(person1.isPersistent())
        Assertions.assertFalse(person2.isPersistent())
        when (persistTest) {
            PersistTest.Iterable -> Person.persist(listOf(person1, person2))
            PersistTest.Stream -> Person.persist(Stream.of(person1, person2))
            PersistTest.Variadic -> Person.persist(person1, person2)
        }
        Assertions.assertTrue(person1.isPersistent())
        Assertions.assertTrue(person2.isPersistent())
    }

    @Inject
    lateinit var personRepository: PersonRepository

    @Inject
    lateinit var dogDao: DogDao

    @Inject
    lateinit var addressDao: AddressDao

    @GET
    @Path("model-dao")
    @Transactional
    fun testModelDao(): String {
        personRepository.flush()
        Assertions.assertNotNull(personRepository.getEntityManager());

        var persons = personRepository.findAll().list()
        Assertions.assertEquals(0, persons.size)

        var personStream = personRepository.findAll().stream()
        Assertions.assertEquals(0, personStream.count())

        try {
            personRepository.findAll().singleResult()
            Assertions.fail<Any>("singleResult should have thrown")
        } catch (x: NoResultException) {
        }

        Assertions.assertNull(personRepository.findAll().firstResult())

        var person: Person = makeSavedPersonDao()
        Assertions.assertNotNull(person.id)

        Assertions.assertEquals(1, personRepository.count())
        Assertions.assertEquals(1, personRepository.count("name = ?1", "stef"))
        Assertions.assertEquals(1, personRepository.count("name = :name", Parameters.with("name", "stef").map()))
        Assertions.assertEquals(1, personRepository.count("name = :name", Parameters.with("name", "stef")))
        Assertions.assertEquals(1, personRepository.count("name", "stef"))

        Assertions.assertEquals(1, dogDao.count())
        Assertions.assertEquals(1, person.dogs.size)

        persons = personRepository.findAll().list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = personRepository.listAll()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        personStream = personRepository.findAll().stream()
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        personStream = personRepository.streamAll()
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        Assertions.assertEquals(person, personRepository.findAll().firstResult())
        Assertions.assertEquals(person, personRepository.findAll().singleResult())

        persons = personRepository.find("name = ?1", "stef").list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = personRepository.find("name = ?1", "stef").withLock(LockModeType.PESSIMISTIC_READ).list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = personRepository.list("name = ?1", "stef")
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = personRepository.find("name = :name", Parameters.with("name", "stef").map()).list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = personRepository.find("name = :name", Parameters.with("name", "stef")).list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = personRepository.list("name = :name", Parameters.with("name", "stef").map())
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = personRepository.list("name = :name", Parameters.with("name", "stef"))
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        persons = personRepository.find("name", "stef").list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(person, persons[0])

        personStream = personRepository.find("name = ?1", "stef").stream()
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        personStream = personRepository.stream("name = ?1", "stef")
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        personStream = personRepository.stream("name = :name", Parameters.with("name", "stef").map())
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        personStream = personRepository.stream("name = :name", Parameters.with("name", "stef"))
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        personStream = personRepository.find("name", "stef").stream()
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()))

        Assertions.assertEquals(person, personRepository.find("name", "stef").firstResult())
        Assertions.assertEquals(person, personRepository.find("name", "stef").singleResult())

        var byId = person.id?.let { personRepository.findById(it) }
        Assertions.assertEquals(person, byId)

        byId = person.id?.let { personRepository.findById(it, LockModeType.PESSIMISTIC_READ) }
        Assertions.assertEquals(person, byId)

        personRepository.delete(person)
        Assertions.assertEquals(0, personRepository.count())

        person = makeSavedPersonDao()
        Assertions.assertEquals(1, personRepository.count())
        Assertions.assertEquals(0, personRepository.delete("name = ?1", "emmanuel"))
        Assertions.assertEquals(1, dogDao.delete("owner = ?1", person))
        Assertions.assertEquals(1, personRepository.delete("name", "stef"))
        person = makeSavedPerson()
        Assertions.assertEquals(1, dogDao.delete("owner = :owner", Parameters.with("owner", person).map()))
        Assertions.assertEquals(1, personRepository.delete("name", "stef"))
        person = makeSavedPerson()
        Assertions.assertEquals(1, dogDao.delete("owner = :owner", Parameters.with("owner", person)))
        Assertions.assertEquals(1, personRepository.delete("name", "stef"))

        Assertions.assertEquals(0, personRepository.deleteAll())

        makeSavedPersonDao()
        Assertions.assertEquals(1, dogDao.deleteAll())
        Assertions.assertEquals(1, personRepository.deleteAll())

        testPersistDao(PersistTest.Iterable)
        testPersistDao(PersistTest.Stream)
        testPersistDao(PersistTest.Variadic)
        Assertions.assertEquals(6, personRepository.deleteAll())

        testSortingDao()

        // paging
        for (i in 0..6) {
            makeSavedPersonDao(i.toString())
        }
        testPaging(personRepository.findAll())
        testPaging(personRepository.find("ORDER BY name"))

        try {
            personRepository.findAll().singleResult()
            Assertions.fail<Any>("singleResult should have thrown")
        } catch (x: NonUniqueResultException) {
        }

        Assertions.assertNotNull(personRepository.findAll().firstResult())

        Assertions.assertEquals(7, personRepository.deleteAll())

        testUpdateDAO()

        //flush
        val person1 = Person()
        person1.name = "testFlush1"
        person1.uniqueName = "unique"
        personRepository.persist(person1)
        val person2 = Person()
        person2.name = "testFlush2"
        person2.uniqueName = "unique"
        try {
            personRepository.persistAndFlush(person2)
            Assertions.fail<Any>()
        } catch (pe: PersistenceException) {
            //this is expected
        }

        return "OK"
    }

    private fun testSortingDao() {
        val person1 = Person()
        person1.name = "stef"
        person1.status = Status.LIVING
        personRepository.persist(person1)

        val person2 = Person()
        person2.name = "stef"
        person2.status = Status.DECEASED
        personRepository.persist(person2)

        val person3 = Person()
        person3.name = "emmanuel"
        person3.status = Status.LIVING
        personRepository.persist(person3)

        val sort1 = Sort.by("name", "status")
        val order1: List<Person> = listOf(person3, person2, person1 )

        var list = personRepository.findAll(sort1).list()
        Assertions.assertEquals(order1, list)

        list = personRepository.listAll(sort1)
        Assertions.assertEquals(order1, list)

        list = personRepository.streamAll(sort1).collect(Collectors.toList())
        Assertions.assertEquals(order1, list)

        val sort2 = Sort.descending("name", "status")
        val order2: List<Person> = listOf(person1, person2)

        list = personRepository.find("name", sort2, "stef").list()
        Assertions.assertEquals(order2, list)

        list = personRepository.list("name", sort2, "stef")
        Assertions.assertEquals(order2, list)

        list = personRepository.stream("name", sort2, "stef").collect(Collectors.toList())
        Assertions.assertEquals(order2, list)

        list = personRepository.find("name = :name", sort2, Parameters.with("name", "stef").map()).list()
        Assertions.assertEquals(order2, list)

        list = personRepository.list("name = :name", sort2, Parameters.with("name", "stef").map())
        Assertions.assertEquals(order2, list)

        list = personRepository.stream("name = :name", sort2, Parameters.with("name", "stef").map()).collect(Collectors.toList())
        Assertions.assertEquals(order2, list)

        list = personRepository.find("name = :name", sort2, Parameters.with("name", "stef")).list()
        Assertions.assertEquals(order2, list)

        list = personRepository.list("name = :name", sort2, Parameters.with("name", "stef"))
        Assertions.assertEquals(order2, list)

        list = personRepository.stream("name = :name", sort2, Parameters.with("name", "stef")).collect(Collectors.toList())
        Assertions.assertEquals(order2, list)

        Assertions.assertEquals(3, Person.deleteAll())
    }

    internal enum class PersistTest {
        Iterable, Variadic, Stream
    }

    private fun testPersistDao(persistTest: PersistTest) {
        val person1 = Person()
        person1.name = "stef1"
        val person2 = Person()
        person2.name = "stef2"
        Assertions.assertFalse(person1.isPersistent())
        Assertions.assertFalse(person2.isPersistent())
        when (persistTest) {
            PersistTest.Iterable -> personRepository.persist(listOf(person1, person2))
            PersistTest.Stream -> personRepository.persist(Stream.of(person1, person2))
            PersistTest.Variadic -> personRepository.persist(person1, person2)
        }
        Assertions.assertTrue(person1.isPersistent())
        Assertions.assertTrue(person2.isPersistent())
    }

    private fun makeSavedPersonDao(suffix: String): Person {
        val person = Person()
        person.name = "stef$suffix"
        person.status = Status.LIVING
        person.address = Address("stef street")
        addressDao.persist(person.address!!)

        personRepository.persist(person)

        return person
    }

    private fun makeSavedPersonDao(): Person {
        val person: Person = makeSavedPersonDao("")

        val dog = Dog("octave", "dalmatian")
        dog.owner = person
        person.dogs.add(dog)
        dogDao.persist(dog)

        return person
    }

    private fun testPaging(query: PanacheQuery<Person>) {
        // ints
        var persons: List<Person> = query.page(0, 3).list()
        Assertions.assertEquals(3, persons.size)
        Assertions.assertEquals("stef0", persons[0].name)
        Assertions.assertEquals("stef1", persons[1].name)
        Assertions.assertEquals("stef2", persons[2].name)

        persons = query.page(1, 3).list()
        Assertions.assertEquals(3, persons.size)
        Assertions.assertEquals("stef3", persons[0].name)
        Assertions.assertEquals("stef4", persons[1].name)
        Assertions.assertEquals("stef5", persons[2].name)

        persons = query.page(2, 3).list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals("stef6", persons[0].name)

        persons = query.page(2, 4).list()
        Assertions.assertEquals(0, persons.size)

        // page
        var page = Page(3)
        persons = query.page(page).list()
        Assertions.assertEquals(3, persons.size)
        Assertions.assertEquals("stef0", persons[0].name)
        Assertions.assertEquals("stef1", persons[1].name)
        Assertions.assertEquals("stef2", persons[2].name)

        page = page.next()
        persons = query.page(page).list()
        Assertions.assertEquals(3, persons.size)
        Assertions.assertEquals("stef3", persons[0].name)
        Assertions.assertEquals("stef4", persons[1].name)
        Assertions.assertEquals("stef5", persons[2].name)

        page = page.next()
        persons = query.page(page).list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals("stef6", persons[0].name)

        page = page.next()
        persons = query.page(page).list()
        Assertions.assertEquals(0, persons.size)

        // query paging
        page = Page(3)
        persons = query.page(page).list()
        Assertions.assertEquals(3, persons.size)
        Assertions.assertEquals("stef0", persons[0].name)
        Assertions.assertEquals("stef1", persons[1].name)
        Assertions.assertEquals("stef2", persons[2].name)
        Assertions.assertTrue(query.hasNextPage())
        Assertions.assertFalse(query.hasPreviousPage())

        persons = query.nextPage().list()
        Assertions.assertEquals(1, query.page().index)
        Assertions.assertEquals(3, query.page().size)
        Assertions.assertEquals(3, persons.size)
        Assertions.assertEquals("stef3", persons[0].name)
        Assertions.assertEquals("stef4", persons[1].name)
        Assertions.assertEquals("stef5", persons[2].name)
        Assertions.assertTrue(query.hasNextPage())
        Assertions.assertTrue(query.hasPreviousPage())

        persons = query.nextPage().list()
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals("stef6", persons[0].name)
        Assertions.assertFalse(query.hasNextPage())
        Assertions.assertTrue(query.hasPreviousPage())

        persons = query.nextPage().list()
        Assertions.assertEquals(0, persons.size)

        Assertions.assertEquals(7, query.count())
        Assertions.assertEquals(3, query.pageCount())
    }

    @GET
    @Path("accessors")
    @Throws(NoSuchMethodException::class, SecurityException::class)
    fun testAccessors(): String {
        checkMethod(AccessorEntity::class.java, "getString", String::class.java)
        checkMethod(AccessorEntity::class.java, "getBool", Boolean::class.javaPrimitiveType)
        checkMethod(AccessorEntity::class.java, "getB", Byte::class.javaPrimitiveType)
        checkMethod(AccessorEntity::class.java, "getC", Char::class.javaPrimitiveType)
        checkMethod(AccessorEntity::class.java, "getS", Short::class.javaPrimitiveType)
        checkMethod(AccessorEntity::class.java, "getI", Int::class.javaPrimitiveType)
        checkMethod(AccessorEntity::class.java, "getL", Long::class.javaPrimitiveType)
        checkMethod(AccessorEntity::class.java, "getF", Float::class.javaPrimitiveType)
        checkMethod(AccessorEntity::class.java, "getD", Double::class.javaPrimitiveType)
        checkMethod(AccessorEntity::class.java, "getT", Any::class.java)
        checkMethod(AccessorEntity::class.java, "getT2", Any::class.java)

        checkMethod(AccessorEntity::class.java, "setString", Void.TYPE, String::class.java)
        checkMethod(AccessorEntity::class.java, "setBool", Void.TYPE, Boolean::class.javaPrimitiveType!!)
        checkMethod(AccessorEntity::class.java, "setC", Void.TYPE, Char::class.javaPrimitiveType!!)
        checkMethod(AccessorEntity::class.java, "setS", Void.TYPE, Short::class.javaPrimitiveType!!)
        checkMethod(AccessorEntity::class.java, "setI", Void.TYPE, Int::class.javaPrimitiveType!!)
        checkMethod(AccessorEntity::class.java, "setL", Void.TYPE, Long::class.javaPrimitiveType!!)
        checkMethod(AccessorEntity::class.java, "setF", Void.TYPE, Float::class.javaPrimitiveType!!)
        checkMethod(AccessorEntity::class.java, "setD", Void.TYPE, Double::class.javaPrimitiveType!!)
        checkMethod(AccessorEntity::class.java, "setT", Void.TYPE, Any::class.java)
        checkMethod(AccessorEntity::class.java, "setT2", Void.TYPE, Any::class.java)

        // Now check that accessors are called
        val entity = AccessorEntity()
        val b: Byte = entity.b
        Assertions.assertEquals(1, entity.getBCalls)
        entity.i = 2
        Assertions.assertEquals(1, entity.setICalls)

        // accessors inside the entity itself
        entity.method()
        Assertions.assertEquals(2, entity.getBCalls)
        Assertions.assertEquals(2, entity.setICalls)

        Assertions.assertThrows(UnsupportedOperationException::class.java) { entity.l }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { entity.l = 42 }

        return "OK"
    }

    private fun checkMethod(klass: Class<*>, name: String, returnType: Class<*>?, vararg params: Class<*>) {
        val method = klass.getMethod(name, *params)
        Assertions.assertEquals(returnType, method.returnType)
    }

    @GET
    @Path("model1")
    @Transactional
    fun testModel1(): String {
        Person.deleteAll()
        Assertions.assertEquals(0, Person.count())

        val person: Person = makeSavedPerson("")
        val trackingPerson = person as SelfDirtinessTracker

        var dirtyAttributes = trackingPerson.`$$_hibernate_getDirtyAttributes`()
        Assertions.assertEquals(0, dirtyAttributes.size)

        person.name = "1"

        dirtyAttributes = trackingPerson.`$$_hibernate_getDirtyAttributes`()
        Assertions.assertEquals(1, dirtyAttributes.size)

        Assertions.assertEquals(1, Person.count())
        return "OK"
    }

    @GET
    @Path("model2")
    @Transactional
    fun testModel2(): String {
        Assertions.assertEquals(1, Person.count())

        val person = Person.findAll().firstResult()
        Assertions.assertEquals("1", person?.name)

        if(person != null) {
            person.name = "2"
        }
        return "OK"
    }

    @GET
    @Path("model3")
    @Transactional
    fun testModel3(): String {
        Assertions.assertEquals(1, Person.count())

        val person = Person.findAll().firstResult()
        Assertions.assertEquals("2", person?.name)

        Dog.deleteAll()
        Person.deleteAll()
        Address.deleteAll()
        Assertions.assertEquals(0, Person.count())

        return "OK"
    }

    @Produces(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
    @GET
    @Path("ignored-properties")
    fun ignoredProperties(): Person {
        Person::class.java.getMethod("\$\$_hibernate_read_id")
        Person::class.java.getMethod("\$\$_hibernate_read_name")
        try {
            Person::class.java.getMethod("\$\$_hibernate_read_persistent")
            Assertions.fail<Any>()
        } catch (e: NoSuchMethodException) {
        }

        // no need to persist it, we can fake it
        val person = Person()
        person.id = 666L
        person.name = "Eddie"
        person.status = Status.DECEASED
        return person
    }

    @Inject
    lateinit var bug5274EntityRepository: Bug5274EntityRepository

    @GET
    @Path("5274")
    @Transactional
    fun testBug5274(): String {
        bug5274EntityRepository.count()
        return "OK"
    }

    @Inject
    lateinit var bug5885EntityRepository: Bug5885EntityRepository

    @GET
    @Path("5885")
    @Transactional
    fun testBug5885(): String {
        bug5885EntityRepository.findById(1L)
        return "OK"
    }

    @GET
    @Path("testJaxbAnnotationTransfer")
    fun testJaxbAnnotationTransfer(): String {
        // Test for fix to this bug: https://github.com/quarkusio/quarkus/issues/6021

        // Ensure that any JAX-B annotations are properly moved to generated getters
        var m: Method = JAXBEntity::class.java.getMethod("getNamedAnnotatedProp")
        var annotation = m.getAnnotation(XmlAttribute::class.java)
        Assertions.assertNotNull(annotation)
        Assertions.assertEquals("Named", annotation.name)
        Assertions.assertNull(m.getAnnotation(XmlTransient::class.java))

        m = JAXBEntity::class.java.getMethod("getDefaultAnnotatedProp")
        annotation = m.getAnnotation(XmlAttribute::class.java)
        Assertions.assertNotNull(annotation)
        Assertions.assertEquals("##default", annotation.name)
        Assertions.assertNull(m.getAnnotation(XmlTransient::class.java))

        m = JAXBEntity::class.java.getMethod("getUnAnnotatedProp")
        Assertions.assertNull(m.getAnnotation(XmlAttribute::class.java))
        Assertions.assertNull(m.getAnnotation(XmlTransient::class.java))

        m = JAXBEntity::class.java.getMethod("getTransientProp")
        Assertions.assertNull(m.getAnnotation(XmlAttribute::class.java))
        Assertions.assertNotNull(m.getAnnotation(XmlTransient::class.java))

        m = JAXBEntity::class.java.getMethod("getArrayAnnotatedProp")
        Assertions.assertNull(m.getAnnotation(XmlTransient::class.java))
        val elementsAnnotation = m.getAnnotation(XmlElements::class.java)
        Assertions.assertNotNull(elementsAnnotation)
        Assertions.assertNotNull(elementsAnnotation.value)
        Assertions.assertEquals(2, elementsAnnotation.value.size)
        Assertions.assertEquals("array1", elementsAnnotation.value[0].name)
        Assertions.assertEquals("array2", elementsAnnotation.value[1].name)

        // Ensure that all original fields were labeled @XmlTransient and had their original JAX-B annotations removed
        ensureFieldSanitized("namedAnnotatedProp")
        ensureFieldSanitized("transientProp")
        ensureFieldSanitized("defaultAnnotatedProp")
        ensureFieldSanitized("unAnnotatedProp")
        ensureFieldSanitized("arrayAnnotatedProp")

        return "OK"
    }

    private fun ensureFieldSanitized(fieldName: String) {
        val f: Field = JAXBEntity::class.java.getField(fieldName)
        Assertions.assertNull(f.getAnnotation(XmlAttribute::class.java))
        Assertions.assertNotNull(f.getAnnotation(XmlTransient::class.java))
    }


    @GET
    @Path("9036")
    @Transactional
    fun testBug9036(): String {
        Person.deleteAll()

        val emptyPerson = Person()
        emptyPerson.persist()

        val deadPerson = Person()
        deadPerson.name = "Stef"
        deadPerson.status = Status.DECEASED
        deadPerson.persist()

        val livePerson = Person()
        livePerson.name = "Stef"
        livePerson.status = Status.LIVING
        livePerson.persist()

        Assertions.assertEquals(3, Person.count())
        Assertions.assertEquals(3, Person.listAll().size)

        // should be filtered
        val query = Person.findAll(Sort.by("id"))
                .filter("Person.isAlive")
                .filter("Person.hasName", Parameters.with("name", "Stef"))
        Assertions.assertEquals(1, query.count())
        Assertions.assertEquals(1, query.list().size)
        Assertions.assertEquals(livePerson, query.list()[0])
        Assertions.assertEquals(1, query.stream().count())
        Assertions.assertEquals(livePerson, query.firstResult())
        Assertions.assertEquals(livePerson, query.singleResult())

        // these should be unaffected
        Assertions.assertEquals(3, Person.count())
        Assertions.assertEquals(3, Person.listAll().size)

        Person.deleteAll()

        return "OK"
    }
}
