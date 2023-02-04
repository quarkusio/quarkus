@file:Suppress("ReactiveStreamsUnusedPublisher")

package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.Panache
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheQuery
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Page.ofSize
import io.quarkus.panache.common.Parameters.with
import io.quarkus.panache.common.Sort
import io.quarkus.panache.common.exception.PanacheQueryException
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.persistence.LockModeType
import jakarta.persistence.NoResultException
import jakarta.persistence.NonUniqueResultException
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.hibernate.engine.spi.SelfDirtinessTracker
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.function.Supplier
import java.util.stream.Stream

@Path("test")
class TestEndpoint {

    @Inject
    lateinit var bug5274EntityRepository: Bug5274EntityRepository

    @Inject
    lateinit var bug5885EntityRepository: Bug5885EntityRepository

    @Inject
    lateinit var personDao: PersonRepository

    @Inject
    lateinit var dogDao: DogDao

    @Inject
    lateinit var addressDao: AddressDao

    @Inject
    lateinit var namedQueryRepository: NamedQueryRepository

    @Inject
    lateinit var namedQueryWith2QueriesRepository: NamedQueryWith2QueriesRepository

    @GET
    @Path("ignored-properties")
    fun ignoredProperties(): Person {
        Person::class.java.getMethod("\$\$_hibernate_read_id")
        Person::class.java.getMethod("\$\$_hibernate_read_name")
        Assertions.assertThrows(NoSuchMethodException::class.java) {
            Person::class.java.getMethod("\$\$_hibernate_read_persistent")
        }

        // no need to persist it, we can fake it
        val person = Person()
        person.id = 666L
        person.name = "Eddie"
        person.status = Status.DECEASED
        return person
    }

    @GET
    @Path("5274")
    fun testBug5274(): Uni<String> {
        return bug5274EntityRepository.count()
            .map { "OK" }
    }

    @GET
    @Path("5885")
    fun testBug5885(): Uni<String> {
        return bug5885EntityRepository.findById(1L)
            .map { "OK" }
    }

    @GET
    @Path("7721")
    fun testBug7721(): Uni<String> {
        val entity = Bug7721Entity()
        return Panache.withTransaction {
            entity.persist<Bug7721Entity>()
                .flatMap { entity.delete() }
                .map { "OK" }
        }
    }

    @GET
    @Path("8254")
    @ReactiveTransactional
    fun testBug8254(): Uni<String> {
        val owner = CatOwner("8254")
        return owner.persist<CatOwner>()
            .flatMap { Cat(owner).persist<Cat>() }
            .flatMap { Cat(owner).persist<Cat>() }
            .flatMap { Cat(owner).persist<Cat>() }
            // This used to fail with an invalid query "SELECT COUNT(*) SELECT DISTINCT cat.owner FROM Cat cat WHERE cat.owner = ?1"
            // Should now result in a valid query "SELECT COUNT(DISTINCT cat.owner) FROM Cat cat WHERE cat.owner = ?1"
            .flatMap {
                CatOwner.find("SELECT DISTINCT cat.owner FROM Cat cat WHERE cat.owner = ?1", owner).count()
            }.flatMap { count ->
                assertEquals(1L, count)
                CatOwner.find("SELECT cat.owner FROM Cat cat WHERE cat.owner = ?1", owner).count()
            }.flatMap { count ->
                assertEquals(3L, count)
                Cat.find("SELECT cat FROM Cat cat WHERE cat.owner = ?1", owner).count()
            }.flatMap { count ->
                assertEquals(3L, count)
                Cat.find("FROM Cat WHERE owner = ?1", owner).count()
            }.flatMap { count ->
                assertEquals(3L, count)
                Cat.find("owner", owner).count()
            }.flatMap { count ->
                assertEquals(3L, count)
                CatOwner.find("name = ?1", "8254").count()
            }.map { count ->
                assertEquals(1L, count)
                "OK"
            }
    }

    @GET
    @Path("9025")
    @ReactiveTransactional
    fun testBug9025(): Uni<String> {
        val apple = Fruit("apple", "red")
        val orange = Fruit("orange", "orange")
        val banana = Fruit("banana", "yellow")
        return Fruit.persist(apple, orange, banana)
            .flatMap {
                val query = Fruit.find("select name, color from Fruit").page(ofSize(1))
                query.list()
                    .flatMap { query.pageCount() }
                    .map { "OK" }
            }
    }

    @GET
    @Path("9036")
    @ReactiveTransactional
    fun testBug9036(): Uni<String> {
        return Person.deleteAll()
            .flatMap { Person().persist<Person>() }
            .flatMap {
                val deadPerson = Person()
                deadPerson.name = "Stef"
                deadPerson.status = Status.DECEASED
                deadPerson.persist<PanacheEntityBase>()
            }.flatMap {
                val livePerson = Person()
                livePerson.name = "Stef"
                livePerson.status = Status.LIVING
                livePerson.persist<PanacheEntityBase>()
            }.flatMap { Person.count() }
            .flatMap { count ->
                assertEquals(3, count)
                Person.listAll()
            }.flatMap { list ->
                assertEquals(3, list.size)
                Person.find("status", Status.LIVING).firstResult()
            }.flatMap { livePerson ->
                // should be filtered
                val query = Person.findAll(Sort.by("id"))
                    .filter("Person.isAlive")
                    .filter("Person.hasName", with("name", "Stef"))
                query.count()
                    .flatMap { count ->
                        assertEquals(1, count)
                        query.list()
                    }.flatMap { list ->
                        assertEquals(1, list.size)
                        assertEquals(livePerson, list[0])
                        query.stream().collect().asList()
                    }.flatMap { list ->
                        assertEquals(1, list.size)
                        query.firstResult()
                    }.flatMap { result ->
                        assertEquals(livePerson, result)
                        query.singleResult()
                    }.flatMap { result ->
                        assertEquals(livePerson, result)
                        Person.count()
                    }.flatMap { count ->
                        assertEquals(3, count)
                        Person.listAll()
                    }.flatMap { list ->
                        assertEquals(3, list.size)
                        Person.deleteAll()
                    }.map { "OK" }
            }
    }

    @GET
    @Path("composite")
    @ReactiveTransactional
    fun testCompositeKey(): Uni<String> {
        val obj = ObjectWithCompositeId()
        obj.part1 = "part1"
        obj.part2 = "part2"
        obj.description = "description"
        return obj.persist<ObjectWithCompositeId>()
            .flatMap {
                val key = ObjectWithCompositeId.ObjectKey("part1", "part2")
                ObjectWithCompositeId.findById(key)
                    .flatMap { result ->
                        Assertions.assertNotNull(result)
                        ObjectWithCompositeId.deleteById(key)
                    }.flatMap { deleted ->
                        assertTrue(deleted)
                        ObjectWithCompositeId.deleteById(key)
                    }.flatMap { deleted ->
                        assertFalse(deleted)
                        val embeddedKey = ObjectWithEmbeddableId.ObjectKey("part1", "part2")
                        val embeddable = ObjectWithEmbeddableId()
                        embeddable.key = embeddedKey
                        embeddable.description = "description"
                        embeddable.persist<ObjectWithEmbeddableId>()
                            .flatMap { ObjectWithEmbeddableId.findById(embeddedKey) }
                            .flatMap { embeddableResult ->
                                Assertions.assertNotNull(embeddableResult)
                                ObjectWithEmbeddableId.deleteById(embeddedKey)
                            }.flatMap { deleted2 ->
                                assertTrue(deleted2)
                                ObjectWithEmbeddableId.deleteById(
                                    ObjectWithEmbeddableId.ObjectKey(
                                        "notexist1",
                                        "notexist2"
                                    )
                                )
                            }.map { deleted2 ->
                                assertFalse(deleted2)
                                "OK"
                            }
                    }
            }
    }

    @GET
    @Path("model")
    @ReactiveTransactional
    fun testModel(): Uni<String> {
        return Person.findAll().list()
            .flatMap { persons ->
                assertEquals(0, persons.size)
                Person.listAll()
            }.flatMap { persons ->
                assertEquals(0, persons.size)
                Person.findAll().stream().collect().asList()
            }.flatMap { personStream ->
                assertEquals(0, personStream.size)
                Person.streamAll().collect().asList()
            }.flatMap { personStream ->
                assertEquals(0, personStream.size)
                assertThrows(NoResultException::class.java) {
                    Person.findAll().singleResult()
                }
            }.flatMap { result ->
                Assertions.assertNull(result)
                makeSavedPerson()
            }.flatMap { person ->
                Assertions.assertNotNull(person.id)
                person.id as Long
                Person.count()
                    .flatMap { count ->
                        assertEquals(1, count)
                        Person.count("name = ?1", "stef")
                    }.flatMap { count ->
                        assertEquals(1, count)
                        Person.count("name = :name", with("name", "stef").map())
                    }.flatMap { count ->
                        assertEquals(1, count)
                        Person.count("name = :name", with("name", "stef"))
                    }.flatMap { count ->
                        assertEquals(1, count)
                        Person.count("name", "stef")
                    }.flatMap { count ->
                        assertEquals(1, count)
                        Dog.count()
                    }.flatMap { count ->
                        assertEquals(1, count)
                        // FIXME: fetch
                        assertEquals(1, person.dogs.size)
                        Person.findAll().list()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.listAll()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.findAll().stream().collect().asList()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.streamAll().collect().asList()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.findAll().firstResult()
                    }.flatMap { personResult ->
                        assertEquals(person, personResult)
                        Person.findAll().singleResult()
                    }.flatMap { personResult ->
                        assertEquals(person, personResult)
                        Person.find("name = ?1", "stef").list()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.find("FROM Person2 WHERE name = ?1", "stef").list()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.find("name = ?1", "stef")
                            .withLock(LockModeType.PESSIMISTIC_READ).list()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.list("name = ?1", "stef")
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.find("name = :name", with("name", "stef").map()).list()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.find("name = :name", with("name", "stef")).list()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.list("name = :name", with("name", "stef").map())
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.find("name = :name", with("name", "stef").map()).list()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.list("name = :name", with("name", "stef"))
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.find("name", "stef").list()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.find("name = ?1", "stef").stream().collect().asList()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.stream("name = ?1", "stef").collect().asList()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.stream("name = :name", with("name", "stef").map()).collect().asList()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.stream("name = :name", with("name", "stef")).collect().asList()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.find("name", "stef").stream().collect().asList()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.find("name", "stef").firstResult()
                    }.flatMap { personResult ->
                        assertEquals(person, personResult)
                        Person.find("name", "stef").singleResult()
                    }.flatMap { personResult ->
                        assertEquals(person, personResult)
                        Person.find("name", "stef").singleResult()
                    }.flatMap { personResult ->
                        assertEquals(person, personResult)
                        Person.list("#Person.getByName", with("name", "stef"))
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        assertThrows(PanacheQueryException::class.java) {
                            Person.find("#Person.namedQueryNotFound").list()
                        }
                    }.flatMap {
                        assertThrows(IllegalArgumentException::class.java) {
                            Person.list(
                                "#Person.getByName",
                                Sort.by("name"),
                                with("name", "stef")
                            )
                        }
                    }
                    .flatMap { NamedQueryEntity.list("#NamedQueryMappedSuperClass.getAll") }
                    .flatMap { NamedQueryEntity.list("#NamedQueryEntity.getAll") }
                    .flatMap { NamedQueryWith2QueriesEntity.list("#NamedQueryWith2QueriesEntity.getAll1") }
                    .flatMap { NamedQueryWith2QueriesEntity.list("#NamedQueryWith2QueriesEntity.getAll2") }
                    .flatMap { Person.find("").list() }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        Person.findById(person.id!!)
                    }.flatMap { byId ->
                        assertEquals(person, byId)
                        assertEquals("Person<" + person.id + ">", byId.toString())
                        Person.findById(person.id!!, LockModeType.PESSIMISTIC_READ)
                    }.flatMap { byId ->
                        assertEquals(person, byId)
                        assertEquals("Person<" + person.id + ">", byId.toString())
                        person.delete()
                    }.flatMap { Person.count() }
                    .flatMap { count ->
                        assertEquals(0, count)
                        makeSavedPerson()
                    }
            }.flatMap { person ->
                Person.count()
                    .flatMap { count ->
                        assertEquals(1, count)
                        Person.delete("name = ?1", "emmanuel")
                    }.flatMap { count ->
                        assertEquals(0, count)
                        Dog.delete("owner = ?1", person)
                    }.flatMap { count ->
                        assertEquals(1, count)
                        Person.delete("name", "stef")
                    }.flatMap { count ->
                        assertEquals(1, count)
                        makeSavedPerson()
                    }
            }.flatMap { person ->
                Dog.delete("owner = :owner", with("owner", person).map())
                    .flatMap { count ->
                        assertEquals(1, count)
                        Person.delete("name", "stef")
                    }.flatMap { count ->
                        assertEquals(1, count)
                        makeSavedPerson()
                    }
            }.flatMap { person ->
                Dog.delete("owner = :owner", with("owner", person))
                    .flatMap { count ->
                        assertEquals(1, count)
                        Person.delete("name", "stef")
                    }.flatMap { count ->
                        assertEquals(1, count)
                        makeSavedPerson()
                    }
            }.flatMap { person ->
                Dog.delete("FROM Dog WHERE owner = :owner", with("owner", person))
                    .flatMap { count ->
                        assertEquals(1, count)
                        Person.delete("FROM Person2 WHERE name = ?1", "stef")
                    }.flatMap { count ->
                        assertEquals(1, count)
                        makeSavedPerson()
                    }
            }.flatMap { person ->
                Dog.delete("DELETE FROM Dog WHERE owner = :owner", with("owner", person))
                    .flatMap { count ->
                        assertEquals(1, count)
                        Person.delete("DELETE FROM Person2 WHERE name = ?1", "stef")
                    }.map { count ->
                        assertEquals(1, count)
                        null
                    }
            }.flatMap { Person.deleteAll() }
            .flatMap { count ->
                assertEquals(0, count)
                makeSavedPerson()
            }.flatMap {
                Dog.deleteAll()
                    .flatMap { count ->
                        assertEquals(1, count)
                        Person.deleteAll()
                    }.map { count ->
                        assertEquals(1, count)
                        null
                    }
            }
            .flatMap { testPersist(PersistTest.Variadic) }
            .flatMap { testPersist(PersistTest.Iterable) }
            .flatMap { testPersist(PersistTest.Stream) }
            .flatMap { Person.deleteAll() }
            .flatMap { count ->
                assertEquals(6, count)
                testSorting()
            } // paging
            .flatMap { makeSavedPerson("0") }
            .flatMap { makeSavedPerson("1") }
            .flatMap { makeSavedPerson("2") }
            .flatMap { makeSavedPerson("3") }
            .flatMap { makeSavedPerson("4") }
            .flatMap { makeSavedPerson("5") }
            .flatMap { makeSavedPerson("6") }
            .flatMap { testPaging(Person.findAll()) }
            .flatMap { testPaging(Person.find("ORDER BY name")) } // range
            .flatMap { testRange(Person.findAll()) }
            .flatMap { testRange(Person.find("ORDER BY name")) }
            .flatMap {
                assertThrows(NonUniqueResultException::class.java) {
                    Person.findAll().singleResult()
                }
            }
            .flatMap { Person.findAll().firstResult() }
            .flatMap { person ->
                Assertions.assertNotNull(person)
                Person.deleteAll()
            }.flatMap { count ->
                assertEquals(7, count)
                testUpdate()
            }.flatMap {
                Person.deleteById(666L) // not existing
            }.flatMap { deleted ->
                assertFalse(deleted)

                // persistAndFlush
                val person1 = Person()
                person1.name = "testFLush1"
                person1.uniqueName = "unique"
                person1.persist<Person>()
            }.flatMap { Person.deleteAll() }
            .map { "OK" }
    }

    @GET
    @Path("model1")
    @ReactiveTransactional
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun testModel1(): Uni<String> {
        return Person.count()
            .flatMap { count ->
                assertEquals(0, count)
                makeSavedPerson("")
            }.flatMap { person ->
                val trackingPerson = person as SelfDirtinessTracker
                var dirtyAttributes = trackingPerson.`$$_hibernate_getDirtyAttributes`()
                assertEquals(0, dirtyAttributes.size)
                person.name = "1"
                dirtyAttributes = trackingPerson.`$$_hibernate_getDirtyAttributes`()
                assertEquals(1, dirtyAttributes.size)
                Person.count()
            }.map { count ->
                assertEquals(1, count)
                "OK"
            }
    }

    @GET
    @Path("model2")
    @ReactiveTransactional
    fun testModel2(): Uni<String> {
        return Person.count()
            .flatMap { count ->
                assertEquals(1, count)
                Person.findAll().firstResult()
            }.map { person ->
                assertEquals("1", person?.name)
                person?.name = "2"
                "OK"
            }
    }

    @GET
    @Path("projection1")
    @ReactiveTransactional
    fun testProjection(): Uni<String> {
        return Person.count()
            .flatMap { count ->
                assertEquals(1, count)
                Person.findAll().project(PersonName::class.java)
                    .firstResult()
            }.flatMap { person ->
                assertEquals("2", person?.name)
                Person.find("name", "2").project(PersonName::class.java)
                    .firstResult()
            }.flatMap { person ->
                assertEquals("2", person?.name)
                Person.find("name = ?1", "2").project(PersonName::class.java)
                    .firstResult()
            }.flatMap { person ->
                assertEquals("2", person?.name)
                Person.find(
                    "name = :name",
                    with("name", "2")
                )
                    .project(PersonName::class.java)
                    .firstResult()
            }.flatMap { person ->
                assertEquals("2", person?.name)
                val query: PanacheQuery<PersonName> =
                    Person.findAll()
                        .project(
                            PersonName::class.java
                        )
                        .page(0, 2)
                query.list()
                    .flatMap { results ->
                        assertEquals(1, results.size)
                        query.nextPage()
                        query.list()
                    }.flatMap { results ->
                        assertEquals(0, results.size)
                        Person.findAll()
                            .project(PersonName::class.java)
                            .count()
                    }.map { count ->
                        assertEquals(1, count)
                        "OK"
                    }
            }
    }

    @GET
    @Path("projection2")
    @ReactiveTransactional
    fun testProjection2(): Uni<String> {
        val ownerName = "Julie"
        val catName = "Bubulle"
        val catWeight = 8.5
        val catOwner = CatOwner(ownerName)
        return catOwner.persist<CatOwner>()
            .flatMap {
                Cat(catName, catOwner, catWeight)
                    .persist<Cat>()
            }.flatMap {
                Cat.find("name", catName)
                    .project(CatDto::class.java)
                    .firstResult()
            }.flatMap { cat ->
                assertEquals(catName, cat?.name)
                assertEquals(ownerName, cat?.ownerName)
                Cat.find(
                    "select c.name, c.owner.name as ownerName from Cat c where c.name = :name",
                    with("name", catName)
                )
                    .project(CatProjectionBean::class.java)
                    .singleResult()
            }.flatMap { catView ->
                assertEquals(catName, catView.name)
                assertEquals(ownerName, catView.ownerName)
                Assertions.assertNull(catView.weight)
                Cat.find("select 'fake_cat', 'fake_owner', 12.5 from Cat c")
                    .project(CatProjectionBean::class.java)
                    .firstResult()
            }.map { catView ->
                assertEquals("fake_cat", catView?.name)
                assertEquals("fake_owner", catView?.ownerName)
                assertEquals(12.5, catView?.weight)
                // The spaces at the beginning are intentional
                Cat.find(
                    "   SELECT c.name, cast(null as string), SUM(c.weight) from Cat c where name = :name group by name  ",
                    with("name", catName)
                )
                    .project(CatProjectionBean::class.java)
            }.flatMap { projectionQuery: PanacheQuery<CatProjectionBean> ->
                projectionQuery
                    .firstResult()
                    .map { catView: CatProjectionBean? ->
                        assertEquals(catName, catView?.name)
                        Assertions.assertNull(catView?.ownerName)
                        assertEquals(catWeight, catView?.weight)
                    }
                projectionQuery.count()
            }.map { count ->
                assertEquals(1L, count)
                // The spaces at the beginning are intentional
                Cat.find(
                    "   SELECT   disTINct  c.name, cast(null as string), SUM(c.weight) from Cat c where " +
                        "name = :name group by name  ",
                    with("name", catName)
                )
                    .project(CatProjectionBean::class.java)
            }.flatMap { projectionQuery ->
                projectionQuery
                    .firstResult()
                    .map { catView ->
                        assertEquals(catName, catView?.name)
                        Assertions.assertNull(catView?.ownerName)
                        assertEquals(catWeight, catView?.weight)
                    }
                projectionQuery.count()
            }.map { count ->
                assertEquals(1L, count)
                val exception = Assertions.assertThrows(PanacheQueryException::class.java) {
                    Cat.find("select new FakeClass('fake_cat', 'fake_owner', 12.5) from Cat c")
                        .project(CatProjectionBean::class.java)
                }
                assertTrue(exception.message!!.startsWith("Unable to perform a projection on a 'select new' query"))
                "OK"
            }
    }

    @GET
    @Path("model3")
    @ReactiveTransactional
    fun testModel3(): Uni<String> {
        return Person.count()
            .flatMap { count ->
                assertEquals(1, count)
                Person.findAll().firstResult()
            }.flatMap { person ->
                assertEquals("2", person?.name)
                Dog.deleteAll()
            }.flatMap { Person.deleteAll() }
            .flatMap { Address.deleteAll() }
            .flatMap { Person.count() }
            .map { count ->
                assertEquals(0, count)
                "OK"
            }
    }

    @GET
    @Path("model-dao")
    @ReactiveTransactional
    fun testModelDao(): Uni<String> {
        return personDao.findAll().list()
            .flatMap { persons ->
                assertEquals(0, persons.size)
                personDao.listAll()
            }.flatMap { persons ->
                assertEquals(0, persons.size)
                personDao.findAll().stream().collect().asList()
            }.flatMap { personStream ->
                assertEquals(0, personStream.size)
                personDao.streamAll().collect().asList()
            }.flatMap { personStream ->
                assertEquals(0, personStream.size)
                assertThrows(NoResultException::class.java) {
                    personDao.findAll().singleResult()
                }
            }.flatMap {
                personDao.findAll().firstResult()
            }.flatMap { result ->
                Assertions.assertNull(result)
                makeSavedPersonDao()
            }
            .flatMap { person ->
                Assertions.assertNotNull(person.id)
                personDao.count()
                    .flatMap { count ->
                        assertEquals(1, count)
                        personDao.count("name = ?1", "stef")
                    }
                    .flatMap { count ->
                        assertEquals(1, count)
                        personDao.count("name = :name", with("name", "stef").map())
                    }
                    .flatMap { count ->
                        assertEquals(1, count)
                        personDao.count("name = :name", with("name", "stef"))
                    }
                    .flatMap { count ->
                        assertEquals(1, count)
                        personDao.count("name", "stef")
                    }
                    .flatMap { count ->
                        assertEquals(1, count)
                        dogDao.count()
                    }
                    .flatMap { count ->
                        assertEquals(1, count)
                        // FIXME: fetch
                        assertEquals(1, person.dogs.size)
                        personDao.findAll().list()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.listAll()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.findAll().stream()
                            .collect().asList()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.streamAll().collect().asList()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.findAll().firstResult()
                    }.flatMap { personResult ->
                        assertEquals(person, personResult)
                        personDao.findAll().singleResult()
                    }
                    .flatMap { personResult ->
                        assertEquals(person, personResult)
                        personDao.find("name = ?1", "stef").list()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.find("name = ?1", "stef")
                            .withLock(LockModeType.PESSIMISTIC_READ)
                            .list()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.list("name = ?1", "stef")
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.find("name = :name", with("name", "stef").map())
                            .list()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.find("name = :name", with("name", "stef"))
                            .list()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.list("name = :name", with("name", "stef").map())
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.find("name = :name", with("name", "stef").map())
                            .list()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.list("name = :name", with("name", "stef"))
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.find("name", "stef").list()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.find("name = ?1", "stef").stream()
                            .collect().asList()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.stream("name = ?1", "stef").collect().asList()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.stream("name = :name", with("name", "stef").map())
                            .collect().asList()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.stream("name = :name", with("name", "stef"))
                            .collect().asList()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.find("name", "stef").stream()
                            .collect().asList()
                    }
                    .flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.find("name", "stef").firstResult()
                    }.flatMap { personResult ->
                        assertEquals(person, personResult)
                        personDao.find("name", "stef").singleResult()
                    }.flatMap { personResult ->
                        assertEquals(person, personResult)
                        personDao.find("name", "stef").singleResult()
                    }
                    .flatMap { personResult ->
                        assertEquals(person, personResult)
                        personDao.list("#Person.getByName", with("name", "stef"))
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        assertThrows(PanacheQueryException::class.java) {
                            personDao.find("#Person.namedQueryNotFound").list()
                        }
                    }.flatMap {
                        assertThrows(IllegalArgumentException::class.java) {
                            personDao.list("#Person.getByName", Sort.by("name"), with("name", "stef"))
                        }
                    }.flatMap {
                        namedQueryRepository.list("#NamedQueryMappedSuperClass.getAll")
                    }.flatMap {
                        namedQueryRepository.list("#NamedQueryEntity.getAll")
                    }.flatMap {
                        namedQueryWith2QueriesRepository.list("#NamedQueryWith2QueriesEntity.getAll1")
                    }.flatMap {
                        namedQueryWith2QueriesRepository.list("#NamedQueryWith2QueriesEntity.getAll2")
                    }.flatMap {
                        personDao.find("").list()
                    }.flatMap { persons ->
                        assertEquals(1, persons.size)
                        assertEquals(person, persons[0])
                        personDao.findById(person.id!!)
                    }.flatMap { byId ->
                        assertEquals(person, byId)
                        assertEquals("Person<${person.id}>", byId.toString())
                        personDao.findById(person.id!!, LockModeType.PESSIMISTIC_READ)
                    }.flatMap { byId ->
                        assertEquals(person, byId)
                        assertEquals("Person<${person.id}>", byId.toString())
                        person.delete()
                    }.flatMap { personDao.count() }
                    .flatMap { count ->
                        assertEquals(0, count)
                        makeSavedPersonDao()
                    }.flatMap {
                        Person.count("#Person.countAll")
                            .flatMap { count ->
                                assertEquals(1, count)
                                Person.count("#Person.countByName", with("name", "stef").map())
                            }.flatMap { count ->
                                assertEquals(1, count)
                                Person.count("#Person.countByName", with("name", "stef"))
                            }.flatMap { count ->
                                assertEquals(1, count)
                                Person.count("#Person.countByName.ordinal", "stef")
                            }.flatMap { count ->
                                assertEquals(1, count)
                                Uni.createFrom().voidItem()
                            }
                    }.flatMap {
                        Person.update("#Person.updateAllNames", with("name", "stef2").map())
                            .flatMap { count ->
                                assertEquals(1, count)
                                Person.find("#Person.getByName", with("name", "stef2")).list()
                            }.flatMap { persons ->
                                assertEquals(1, persons.size)
                                Person.update("#Person.updateAllNames", with("name", "stef3"))
                            }.flatMap { count ->
                                assertEquals(1, count)
                                Person.find("#Person.getByName", with("name", "stef3")).list()
                            }.flatMap { persons ->
                                assertEquals(1, persons.size)
                                Person.update(
                                    "#Person.updateNameById",
                                    with("name", "stef2")
                                        .and("id", persons[0].id).map()
                                )
                            }.flatMap { count ->
                                assertEquals(1, count)
                                Person.find("#Person.getByName", with("name", "stef2")).list()
                            }.flatMap { persons ->
                                assertEquals(1, persons.size)
                                Person.update(
                                    "#Person.updateNameById",
                                    with("name", "stef3")
                                        .and("id", persons[0].id)
                                )
                            }.flatMap { count ->
                                assertEquals(1, count)
                                Person.find("#Person.getByName", with("name", "stef3")).list()
                            }.flatMap { persons ->
                                assertEquals(1, persons.size)
                                Person.update("#Person.updateNameById.ordinal", "stef", persons[0].id!!)
                            }.flatMap { count ->
                                assertEquals(1, count)
                                Person.find("#Person.getByName", with("name", "stef")).list()
                            }.flatMap { persons ->
                                assertEquals(1, persons.size)
                                Uni.createFrom().voidItem()
                            }
                    }.flatMap {
                        Dog.deleteAll()
                            .flatMap {
                                Person.delete("#Person.deleteAll")
                            }.flatMap { count ->
                                assertEquals(1, count)
                                Person.find("").list()
                            }.flatMap { persons ->
                                assertEquals(0, persons.size)
                                Uni.createFrom().voidItem()
                            }
                    }.flatMap {
                        makeSavedPerson().flatMap { personToDelete ->
                            Dog.deleteAll()
                                .flatMap {
                                    Person.find("").list()
                                }.flatMap { persons ->
                                    assertEquals(1, persons.size)
                                    Person.delete("#Person.deleteById", with("id", personToDelete.id).map())
                                }.flatMap { count ->
                                    assertEquals(1, count)
                                    Person.find("").list()
                                }.flatMap { persons ->
                                    assertEquals(0, persons.size)
                                    Uni.createFrom().voidItem()
                                }
                        }
                    }.flatMap {
                        makeSavedPerson().flatMap { personToDelete ->
                            Dog.deleteAll()
                                .flatMap {
                                    Person.find("").list()
                                }.flatMap { persons ->
                                    assertEquals(1, persons.size)
                                    Person.delete("#Person.deleteById", with("id", personToDelete.id))
                                }.flatMap { count ->
                                    assertEquals(1, count)
                                    Person.find("").list()
                                }.flatMap { persons ->
                                    assertEquals(0, persons.size)
                                    Uni.createFrom().voidItem()
                                }
                        }
                    }.flatMap {
                        makeSavedPerson().flatMap { personToDelete ->
                            Dog.deleteAll()
                                .flatMap {
                                    Person.find("").list()
                                }.flatMap { persons ->
                                    assertEquals(1, persons.size)
                                    Person.delete("#Person.deleteById.ordinal", personToDelete.id!!)
                                }.flatMap { count ->
                                    assertEquals(1, count)
                                    Person.find("").list()
                                }.flatMap { persons ->
                                    assertEquals(0, persons.size)
                                    makeSavedPersonDao()
                                }
                        }
                    }
            }.flatMap { person ->
                personDao.count()
                    .flatMap { count ->
                        assertEquals(1, count)
                        personDao.delete("name = ?1", "emmanuel")
                    }.flatMap { count ->
                        assertEquals(0, count)
                        dogDao.delete("owner = ?1", person)
                    }.flatMap { count ->
                        assertEquals(1, count)
                        personDao.delete("name", "stef")
                    }.flatMap { count ->
                        assertEquals(1, count)
                        makeSavedPersonDao()
                    }
            }.flatMap { person ->
                dogDao.delete("owner = :owner", with("owner", person).map())
                    .flatMap { count ->
                        assertEquals(1, count)
                        personDao.delete("name", "stef")
                    }.flatMap { count ->
                        assertEquals(1, count)
                        makeSavedPersonDao()
                    }
            }.flatMap { person ->
                dogDao.delete("owner = :owner", with("owner", person))
                    .flatMap { count ->
                        assertEquals(1, count)
                        personDao.delete("name", "stef")
                    }.flatMap { count ->
                        assertEquals(1, count)
                        makeSavedPersonDao()
                    }
            }.flatMap { person ->
                dogDao.delete("FROM Dog WHERE owner = :owner", with("owner", person))
                    .flatMap { count ->
                        assertEquals(1, count)
                        personDao.delete("FROM Person2 WHERE name = ?1", "stef")
                    }.flatMap { count ->
                        assertEquals(1, count)
                        makeSavedPersonDao()
                    }
            }.flatMap { person ->
                dogDao.delete("DELETE FROM Dog WHERE owner = :owner", with("owner", person))
                    .flatMap { count ->
                        assertEquals(1, count)
                        personDao.delete("DELETE FROM Person2 WHERE name = ?1", "stef")
                    }.map { count ->
                        assertEquals(1, count)
                        null
                    }
            }.flatMap { v -> personDao.deleteAll() }
            .flatMap { count ->
                assertEquals(0, count)
                makeSavedPersonDao()
            }.flatMap { person ->
                dogDao.deleteAll()
                    .flatMap { count ->
                        assertEquals(1, count)
                        personDao.deleteAll()
                    }.map { count ->
                        assertEquals(1, count)
                        null
                    }
            }
            .flatMap { testPersistDao(PersistTest.Iterable) }
            .flatMap { v -> testPersistDao(PersistTest.Stream) }
            .flatMap { v -> testPersistDao(PersistTest.Variadic) }
            .flatMap { personDao.deleteAll() }
            .flatMap { count ->
                assertEquals(6, count)
                testSorting()
            }.flatMap {
                makeSavedPersonDao("0")
            }.flatMap {
                makeSavedPersonDao("1")
            }.flatMap {
                makeSavedPersonDao("2")
            }.flatMap {
                makeSavedPersonDao("3")
            }.flatMap {
                makeSavedPersonDao("4")
            }.flatMap {
                makeSavedPersonDao("5")
            }.flatMap {
                makeSavedPersonDao("6")
            }.flatMap {
                testPaging(personDao.findAll())
            }.flatMap { v -> testPaging(personDao.find("ORDER BY name")) } // range
            .flatMap { testRange(personDao.findAll()) }
            .flatMap { testRange(personDao.find("ORDER BY name")) }
            .flatMap {
                assertThrows(NonUniqueResultException::class.java) {
                    personDao.findAll().singleResult()
                }
            }.flatMap {
                personDao.findAll().firstResult()
            }.flatMap { person ->
                Assertions.assertNotNull(person)
                personDao.deleteAll()
            }.flatMap { count ->
                assertEquals(7, count)
                testUpdateDao()
            }.flatMap {
                // delete by id
                val toRemove = Person()
                toRemove.name = "testDeleteById"
                toRemove.uniqueName = "testDeleteByIdUnique"
                toRemove.persist<Person>()
                    .flatMap {
                        personDao.deleteById(toRemove.id!!)
                    }
            }.flatMap { deleted ->
                assertTrue(deleted)
                personDao.deleteById(666L) // not existing
            }.flatMap { deleted ->
                assertFalse(deleted)

                // persistAndFlush
                val person1 = Person()
                person1.name = "testFLush1"
                person1.uniqueName = "unique"
                personDao.persistAndFlush(person1)
            }.flatMap {
                personDao.deleteAll()
            }.map {
                "OK"
            }
    }

    @GET
    @Path("testSortByNullPrecedence")
    @ReactiveTransactional
    fun testSortByNullPrecedence(): Uni<String> {
        return Person.deleteAll()
            .flatMap {
                val stefPerson = Person()
                stefPerson.name = "Stef"
                stefPerson.uniqueName = "stef"
                val josePerson = Person()
                josePerson.name = null
                josePerson.uniqueName = "jose"
                Person.persist(stefPerson, josePerson)
            }
            .flatMap {
                Person.findAll(Sort.by("name", Sort.NullPrecedence.NULLS_FIRST)).list()
            }.flatMap { list ->
                assertEquals("jose", list[0].uniqueName)
                Person.findAll(Sort.by("name", Sort.NullPrecedence.NULLS_LAST)).list()
            }.flatMap { list ->
                assertEquals("jose", list[list.size - 1].uniqueName)
                Person.deleteAll()
            }.map { "OK" }
    }

    private fun makeSavedPerson(): Uni<Person> {
        val uni: Uni<Person> = makeSavedPerson("")
        return uni.flatMap { person ->
            val dog = Dog("octave", "dalmatian")
            dog.owner = person
            person.dogs.add(dog)
            dog.persist<Dog>()
                .map { person }
        }
    }

    private fun makeSavedPerson(suffix: String = ""): Uni<Person> {
        val address = Address()
        address.street = "stef street"
        val person = Person()
        person.name = "stef$suffix"
        person.status = Status.LIVING
        person.address = address

        return person.address!!.persist<Address>()
            .flatMap { person.persist() }
    }

    private fun assertThrows(exceptionClass: Class<out Throwable>, f: Supplier<Uni<*>>): Uni<Void> {
        val uni = try {
            f.get()
        } catch (t: Throwable) {
            Uni.createFrom().failure<Any>(t)
        }
        return uni
            .onItemOrFailure()
            .invoke { r: Any?, t: Throwable? -> System.err.println("Got back val: $r and exception $t") }
            .onItem().invoke { _ -> Assertions.fail("Did not throw " + exceptionClass.name) }
            .onFailure(exceptionClass)
            .recoverWithItem { null }
            .map { null }
    }

    private fun testPersist(persistsTest: PersistTest): Uni<Void> {
        val person1 = Person()
        person1.name = "stef1"
        val person2 = Person()
        person2.name = "stef2"

        assertFalse(person1.isPersistent())
        assertFalse(person2.isPersistent())
        return when (persistsTest) {
            PersistTest.Iterable -> Person.persist(listOf(person1, person2))
            PersistTest.Stream -> Person.persist(Stream.of(person1, person2))
            PersistTest.Variadic -> Person.persist(person1, person2)
        }.map {
            assertTrue(person1.isPersistent())
            assertTrue(person2.isPersistent())
            null
        }
    }

    private fun testSorting(): Uni<Unit> {
        val person1 = Person()
        person1.name = "stef"
        person1.status = Status.LIVING
        val person2 = Person()
        person2.name = "stef"
        person2.status = Status.DECEASED
        val person3 = Person()
        person3.name = "emmanuel"
        person3.status = Status.LIVING
        return person1.persist<Person>()
            .flatMap { person2.persist<Person>() }
            .flatMap { person3.persist<Person>() }
            .flatMap {
                val sort1 = Sort.by("name", "status")
                val order1 = listOf(person3, person2, person1)
                val sort2 = Sort.descending("name", "status")
                val order2 = listOf(person1, person2)
                Person.findAll(sort1).list()
                    .flatMap { list ->
                        assertEquals(order1, list)
                        Person.listAll(sort1)
                    }.flatMap { list ->
                        assertEquals(order1, list)
                        Person.streamAll(sort1).collect().asList()
                    }.flatMap { list ->
                        assertEquals(order1, list)
                        Person.find("name", sort2, "stef").list()
                    }.flatMap { list ->
                        assertEquals(order2, list)
                        Person.list("name", sort2, "stef")
                    }.flatMap { list ->
                        assertEquals(order2, list)
                        Person.stream("name", sort2, "stef").collect().asList()
                    }.flatMap { list ->
                        assertEquals(order2, list)
                        Person.find("name = :name", sort2, with("name", "stef").map())
                            .list()
                    }.flatMap { list ->
                        assertEquals(order2, list)
                        Person.list("name = :name", sort2, with("name", "stef").map())
                    }.flatMap { list ->
                        assertEquals(order2, list)
                        Person.stream("name = :name", sort2, with("name", "stef").map())
                            .collect().asList()
                    }.flatMap { list ->
                        assertEquals(order2, list)
                        Person.find("name = :name", sort2, with("name", "stef")).list()
                    }.flatMap { list ->
                        assertEquals(order2, list)
                        Person.list("name = :name", sort2, with("name", "stef"))
                    }.flatMap { list ->
                        assertEquals(order2, list)
                        Person.stream("name = :name", sort2, with("name", "stef"))
                            .collect().asList()
                    }
            }.flatMap { Person.deleteAll() }
            .map { count ->
                assertEquals(3, count)
            }
    }

    private fun testPaging(query: PanacheQuery<Person>): Uni<Void> {
        // No paging allowed until a page is set up
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.firstPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.previousPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.nextPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.lastPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.hasNextPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.hasPreviousPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.page() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.pageCount() }

        return query.page(0, 3).list()
            .flatMap { persons: List<Person> ->
                assertEquals(3, persons.size)
                assertEquals("stef0", persons[0].name)
                assertEquals("stef1", persons[1].name)
                assertEquals("stef2", persons[2].name)
                query.page(1, 3).list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(3, persons.size)
                assertEquals("stef3", persons[0].name)
                assertEquals("stef4", persons[1].name)
                assertEquals("stef5", persons[2].name)
                query.page(2, 3).list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(1, persons.size)
                assertEquals("stef6", persons[0].name)
                query.page(2, 4).list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(0, persons.size)
                query.page(Page(3)).list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(3, persons.size)
                assertEquals("stef0", persons[0].name)
                assertEquals("stef1", persons[1].name)
                assertEquals("stef2", persons[2].name)
                query.page(Page(1, 3)).list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(3, persons.size)
                assertEquals("stef3", persons[0].name)
                assertEquals("stef4", persons[1].name)
                assertEquals("stef5", persons[2].name)
                query.page(Page(2, 3)).list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(1, persons.size)
                assertEquals("stef6", persons[0].name)
                query.page(Page(3, 3)).list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(0, persons.size)

                query.page(Page(3)).list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(3, persons.size)
                assertEquals("stef0", persons[0].name)
                assertEquals("stef1", persons[1].name)
                assertEquals("stef2", persons[2].name)
                query.hasNextPage()
            }
            .flatMap { hasNextPage: Boolean ->
                assertTrue(hasNextPage)
                assertFalse(query.hasPreviousPage())
                query.nextPage().list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(1, query.page().index)
                assertEquals(3, query.page().size)
                assertEquals(3, persons.size)
                assertEquals("stef3", persons[0].name)
                assertEquals("stef4", persons[1].name)
                assertEquals("stef5", persons[2].name)
                query.hasNextPage()
            }
            .flatMap { hasNextPage: Boolean ->
                assertTrue(hasNextPage)
                assertTrue(query.hasPreviousPage())
                query.nextPage().list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(1, persons.size)
                assertEquals("stef6", persons[0].name)
                query.hasNextPage()
            }
            .flatMap { hasNextPage: Boolean ->
                assertFalse(hasNextPage)
                assertTrue(query.hasPreviousPage())
                query.nextPage().list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(0, persons.size)
                query.count()
            }.flatMap { count ->
                assertEquals(7, count)
                query.pageCount()
            }
            .flatMap { count ->
                assertEquals(3, count)
                query.page(0, 3)
                    .range(0, 1)
                    .list()
            }
            .map { persons: List<Person> ->
                assertEquals(2, persons.size)
                assertEquals("stef0", persons[0].name)
                assertEquals("stef1", persons[1].name)
                null
            }
    }

    private fun testRange(query: PanacheQuery<Person>): Uni<Void> {
        return query.range(0, 2).list()
            .flatMap { persons: List<Person> ->
                assertEquals(3, persons.size)
                assertEquals("stef0", persons[0].name)
                assertEquals("stef1", persons[1].name)
                assertEquals("stef2", persons[2].name)
                query.range(3, 5).list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(3, persons.size)
                assertEquals("stef3", persons[0].name)
                assertEquals("stef4", persons[1].name)
                assertEquals("stef5", persons[2].name)
                query.range(6, 8).list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(1, persons.size)
                assertEquals("stef6", persons[0].name)
                query.range(8, 12).list()
            }
            .flatMap { persons: List<Person> ->
                assertEquals(0, persons.size)

                // mix range with page
                Assertions.assertThrows(UnsupportedOperationException::class.java) {
                    query.range(0, 2)
                        .nextPage()
                }
                Assertions.assertThrows(UnsupportedOperationException::class.java) {
                    query.range(0, 2)
                        .previousPage()
                }
                Assertions.assertThrows(UnsupportedOperationException::class.java) {
                    query.range(0, 2).pageCount()
                }
                //                    Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).lastPage());
                Assertions.assertThrows(UnsupportedOperationException::class.java) {
                    query.range(0, 2).firstPage()
                }
                Assertions.assertThrows(UnsupportedOperationException::class.java) {
                    query.range(0, 2).hasPreviousPage()
                }
                Assertions.assertThrows(UnsupportedOperationException::class.java) {
                    query.range(0, 2).hasNextPage()
                }
                Assertions.assertThrows(UnsupportedOperationException::class.java) {
                    query.range(0, 2).page()
                }
                query.range(0, 2)
                    .page(0, 3)
                    .list()
            }
            .map { persons: List<Person> ->
                assertEquals(3, persons.size)
                assertEquals("stef0", persons[0].name)
                assertEquals("stef1", persons[1].name)
                assertEquals("stef2", persons[2].name)
                null
            }
    }

    private fun testUpdate(): Uni<Void> {
        return makeSavedPerson("p1")
            .flatMap {
                makeSavedPerson("p2")
            }.flatMap {
                Person.update("update from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1")
            }.flatMap { updateByIndexParameter ->
                assertEquals(1, updateByIndexParameter, "More than one Person updated")
                Person.update(
                    "update from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                    with("pName", "stefp2")
                        .map()
                )
            }.flatMap { updateByNamedParameter ->
                assertEquals(1, updateByNamedParameter, "More than one Person updated")
                Person.deleteAll()
            }.flatMap { count ->
                assertEquals(2L, count)
                makeSavedPerson("p1")
            }.flatMap {
                makeSavedPerson("p2")
            }.flatMap {
                Person.update("from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1")
            }.flatMap { updateByIndexParameter ->
                assertEquals(1, updateByIndexParameter, "More than one Person updated")
                Person.update(
                    "from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                    with("pName", "stefp2").map()
                )
            }.flatMap { updateByNamedParameter ->
                assertEquals(1, updateByNamedParameter, "More than one Person updated")
                Person.deleteAll()
            }.flatMap { count ->
                assertEquals(2, count)
                makeSavedPerson("p1")
            }.flatMap {
                makeSavedPerson("p2")
            }.flatMap {
                Person.update("set name = 'stefNEW' where name = ?1", "stefp1")
            }.flatMap { updateByIndexParameter ->
                assertEquals(1, updateByIndexParameter, "More than one Person updated")
                Person.update(
                    "set name = 'stefNEW' where name = :pName",
                    with("pName", "stefp2").map()
                )
            }.flatMap { updateByNamedParameter ->
                assertEquals(1, updateByNamedParameter, "More than one Person updated")
                Person.deleteAll()
            }.flatMap { count ->
                assertEquals(2, count)
                makeSavedPerson("p1")
            }.flatMap {
                makeSavedPerson("p2")
            }.flatMap {
                Person.update("name = 'stefNEW' where name = ?1", "stefp1")
            }.flatMap { updateByIndexParameter: Any ->
                assertEquals(1, updateByIndexParameter, "More than one Person updated")
                Person.update("name = 'stefNEW' where name = :pName", with("pName", "stefp2").map())
            }.flatMap { updateByNamedParameter ->
                assertEquals(1, updateByNamedParameter, "More than one Person updated")
                Person.deleteAll()
            }.flatMap { count ->
                assertEquals(2, count)
                makeSavedPerson("p1")
            }.flatMap { makeSavedPerson("p2") }
            .flatMap {
                Person.update("name = 'stefNEW' where name = ?1", "stefp1")
            }.flatMap { updateByIndexParameter ->
                assertEquals(1, updateByIndexParameter, "More than one Person updated")
                Person.update("name = 'stefNEW' where name = :pName", with("pName", "stefp2"))
            }.flatMap { updateByNamedParameter ->
                assertEquals(1, updateByNamedParameter, "More than one Person updated")
                Person.deleteAll()
            }.flatMap {
                assertThrows(PanacheQueryException::class.java) {
                    Person.update(" ")
                }
            }
    }

    private fun makeSavedPersonDao(): Uni<Person> {
        val uni: Uni<Person> = makeSavedPersonDao("")
        return uni.flatMap { person: Person ->
            val dog = Dog("octave", "dalmatian")
            dog.owner = person
            person.dogs.add(dog)
            dog.persist<Dog>()
                .map { person }
        }
    }

    private fun makeSavedPersonDao(suffix: String): Uni<Person> {
        val person = Person()
        person.name = "stef$suffix"
        person.status = Status.LIVING
        person.address = Address("stef street")
        return addressDao.persist(person.address!!)
            .flatMap { personDao.persist(person) }
    }

    private fun testPersistDao(persistTest: PersistTest): Uni<Void> {
        val person1 = Person()
        person1.name = "stef1"
        val person2 = Person()
        person2.name = "stef2"
        assertFalse(personDao.isPersistent(person1))
        assertFalse(personDao.isPersistent(person2))
        val persist = when (persistTest) {
            PersistTest.Iterable -> personDao.persist(listOf(person1, person2))
            PersistTest.Stream -> personDao.persist(Stream.of(person1, person2))
            PersistTest.Variadic -> personDao.persist(person1, person2)
        }
        return persist.map {
            assertTrue(personDao.isPersistent(person1))
            assertTrue(personDao.isPersistent(person2))
            null
        }
    }

    private fun testUpdateDao(): Uni<Void> {
        return makeSavedPersonDao("p1")
            .flatMap {
                makeSavedPersonDao("p2")
            }.flatMap {
                personDao.update(
                    "update from Person2 p set p.name = 'stefNEW' where p.name = ?1",
                    "stefp1"
                )
            }.flatMap { updateByIndexParameter ->
                assertEquals(1, updateByIndexParameter, "More than one Person updated")
                personDao.update(
                    "update from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                    with("pName", "stefp2").map()
                )
            }.flatMap { updateByNamedParameter ->
                assertEquals(1, updateByNamedParameter, "More than one Person updated")
                personDao.deleteAll()
            }.flatMap { count ->
                assertEquals(2, count)
                makeSavedPersonDao("p1")
            }.flatMap {
                makeSavedPersonDao("p2")
            }.flatMap {
                personDao.update("from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1")
            }.flatMap { updateByIndexParameter ->
                assertEquals(1, updateByIndexParameter, "More than one Person updated")
                personDao.update(
                    "from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                    with("pName", "stefp2").map()
                )
            }.flatMap { updateByNamedParameter ->
                assertEquals(1, updateByNamedParameter, "More than one Person updated")
                personDao.deleteAll()
            }.flatMap { count ->
                assertEquals(2, count)
                makeSavedPersonDao("p1")
            }.flatMap {
                makeSavedPersonDao("p2")
            }.flatMap {
                personDao.update("set name = 'stefNEW' where name = ?1", "stefp1")
            }.flatMap { updateByIndexParameter ->
                assertEquals(1, updateByIndexParameter, "More than one Person updated")
                personDao.update(
                    "set name = 'stefNEW' where name = :pName",
                    with("pName", "stefp2").map()
                )
            }.flatMap { updateByNamedParameter ->
                assertEquals(1, updateByNamedParameter, "More than one Person updated")
                personDao.deleteAll()
            }.flatMap { count ->
                assertEquals(2, count)
                makeSavedPersonDao("p1")
            }.flatMap {
                makeSavedPersonDao("p2")
            }.flatMap {
                personDao.update("name = 'stefNEW' where name = ?1", "stefp1")
            }.flatMap { updateByIndexParameter ->
                assertEquals(1, updateByIndexParameter, "More than one Person updated")
                personDao.update(
                    "name = 'stefNEW' where name = :pName",
                    with("pName", "stefp2").map()
                )
            }.flatMap { updateByNamedParameter ->
                assertEquals(1, updateByNamedParameter, "More than one Person updated")
                personDao.deleteAll()
            }.flatMap { count ->
                assertEquals(2, count)
                makeSavedPersonDao("p1")
            }.flatMap {
                makeSavedPersonDao("p2")
            }.flatMap {
                personDao.update("name = 'stefNEW' where name = ?1", "stefp1")
            }.flatMap { updateByIndexParameter ->
                assertEquals(1, updateByIndexParameter, "More than one Person updated")
                personDao.update(
                    "name = 'stefNEW' where name = :pName",
                    with("pName", "stefp2")
                )
            }.flatMap { updateByNamedParameter ->
                assertEquals(1, updateByNamedParameter, "More than one Person updated")
                personDao.deleteAll()
            }.flatMap { count ->
                assertEquals(2, count)
                assertThrows(PanacheQueryException::class.java) { personDao.update(" ") }
            }
    }

    enum class PersistTest {
        Iterable, Variadic, Stream
    }
}
