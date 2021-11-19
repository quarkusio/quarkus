package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions
import java.util.function.Supplier
import javax.persistence.NoResultException
import javax.ws.rs.GET
import javax.ws.rs.Path

/**
 * Various tests covering Panache functionality. All tests should work in both standard JVM and in native mode.
 */
@Path("test")
class TestEndpoint() {
    @ReactiveTransactional
    @GET
    @Path("model")
    fun testModel(): Uni<String> {
        return Person.findAll()
            .list()
            .flatMap { persons ->
                Assertions.assertEquals(0, persons.size)
                Person.listAll()
            }
            .flatMap { persons ->
                Assertions.assertEquals(0, persons.size)
                collect(Person.findAll().stream())
            }
            .flatMap { personStream ->
                Assertions.assertEquals(0, personStream.size)
                collect(Person.streamAll())
            }
            .flatMap { personStream ->
                Assertions.assertEquals(0, personStream.size)
                assertThrows(
                    NoResultException::class.java,
                    { Person.findAll().singleResult() },
                    "No result should be returned",
                )
            }.flatMap {
                Person.findAll()
                    .firstResult()
            }
            .flatMap { result: Any ->
                Assertions.assertNull(result)
                makeSavedPerson()
            }
//            .flatMap(Function<Person, Uni<out Person?>> { person: Person ->
//                Assertions.assertNotNull(person.id)
//                Person.count()
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        Person.count("name = ?1", "stef")
//                    })
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        Person.count(
//                            "name = :name",
//                            Parameters.with("name", "stef").map()
//                        )
//                    })
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        Person.count(
//                            "name = :name",
//                            Parameters.with("name", "stef")
//                        )
//                    })
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        Person.count("name", "stef")
//                    })
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        Dog.count()
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<Long, Uni<out MutableList<PanacheEntityBase?>?>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        // FIXME: fetch
//                        Assertions.assertEquals(1, person.dogs.size)
//                        Person.findAll<PanacheEntityBase>()
//                            .list<PanacheEntityBase>()
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.listAll<PanacheEntityBase>()
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        collect<PanacheEntityBase>(
//                            Person.findAll<PanacheEntityBase>()
//                                .stream<PanacheEntityBase>()
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        collect<PanacheEntityBase>(Person.streamAll<PanacheEntityBase>())
//                    })
//                    .flatMap<Any>(Function<List<PanacheEntityBase?>, Uni<*>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.findAll<PanacheEntityBase>()
//                            .firstResult<PanacheEntityBase>()
//                    })
//                    .flatMap<Any>(Function<Any, Uni<*>> { personResult: Any? ->
//                        Assertions.assertEquals(person, personResult)
//                        Person.findAll<PanacheEntityBase>()
//                            .singleResult<PanacheEntityBase>()
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<Any, Uni<out MutableList<PanacheEntityBase?>?>> { personResult: Any? ->
//                        Assertions.assertEquals(person, personResult)
//                        Person.find<PanacheEntityBase>(
//                            "name = ?1",
//                            "stef"
//                        ).list<PanacheEntityBase>()
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.find<PanacheEntityBase>(
//                            "FROM Person2 WHERE name = ?1",
//                            "stef"
//                        ).list<PanacheEntityBase>()
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.find<PanacheEntityBase>(
//                            "name = ?1",
//                            "stef"
//                        )
//                            .withLock<PanacheEntityBase>(LockModeType.PESSIMISTIC_READ)
//                            .list<PanacheEntityBase>()
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.list<PanacheEntityBase>(
//                            "name = ?1",
//                            "stef"
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.find<PanacheEntityBase>(
//                            "name = :name",
//                            Parameters.with("name", "stef").map()
//                        ).list<PanacheEntityBase>()
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.find<PanacheEntityBase>(
//                            "name = :name",
//                            Parameters.with("name", "stef")
//                        ).list<PanacheEntityBase>()
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.list<PanacheEntityBase>(
//                            "name = :name",
//                            Parameters.with("name", "stef").map()
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.find<PanacheEntityBase>(
//                            "name = :name",
//                            Parameters.with("name", "stef").map()
//                        ).list<PanacheEntityBase>()
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.list<PanacheEntityBase>(
//                            "name = :name",
//                            Parameters.with("name", "stef")
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.find<PanacheEntityBase>(
//                            "name",
//                            "stef"
//                        ).list<PanacheEntityBase>()
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        collect<PanacheEntityBase>(
//                            Person.find<PanacheEntityBase>(
//                                "name = ?1",
//                                "stef"
//                            ).stream<PanacheEntityBase>()
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        collect<PanacheEntityBase>(
//                            Person.stream<PanacheEntityBase>(
//                                "name = ?1",
//                                "stef"
//                            )
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        collect<PanacheEntityBase>(
//                            Person.stream<PanacheEntityBase>(
//                                "name = :name",
//                                Parameters.with("name", "stef").map()
//                            )
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        collect<PanacheEntityBase>(
//                            Person.stream<PanacheEntityBase>(
//                                "name = :name",
//                                Parameters.with("name", "stef")
//                            )
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        collect<PanacheEntityBase>(
//                            Person.find<PanacheEntityBase>(
//                                "name",
//                                "stef"
//                            ).stream<PanacheEntityBase>()
//                        )
//                    })
//                    .flatMap<Any>(Function<List<PanacheEntityBase?>, Uni<*>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.find<PanacheEntityBase>(
//                            "name",
//                            "stef"
//                        ).firstResult<PanacheEntityBase>()
//                    })
//                    .flatMap<Any>(Function<Any, Uni<*>> { personResult: Any? ->
//                        Assertions.assertEquals(person, personResult)
//                        Person.find<PanacheEntityBase>(
//                            "name",
//                            "stef"
//                        ).singleResult<PanacheEntityBase>()
//                    })
//                    .flatMap<Any>(Function<Any, Uni<*>> { personResult: Any? ->
//                        Assertions.assertEquals(person, personResult)
//                        Person.find<PanacheEntityBase>(
//                            "name",
//                            "stef"
//                        ).singleResult<PanacheEntityBase>()
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<Any, Uni<out MutableList<PanacheEntityBase?>?>> { personResult: Any? ->
//                        Assertions.assertEquals(person, personResult)
//                        Person.list<PanacheEntityBase>(
//                            "#Person.getByName",
//                            Parameters.with("name", "stef")
//                        )
//                    })
//                    .flatMap<Void>(Function<List<PanacheEntityBase?>, Uni<out Void>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        assertThrows(
//                            PanacheQueryException::class.java,
//                            Supplier<Uni<*>> {
//                                Person.find<PanacheEntityBase>(
//                                    "#Person.namedQueryNotFound"
//                                ).list<PanacheEntityBase>()
//                            },
//                            "singleResult should have thrown"
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<Void, Uni<out MutableList<PanacheEntityBase?>?>> { v: Void? ->
//                        NamedQueryEntity.list<PanacheEntityBase>(
//                            "#NamedQueryMappedSuperClass.getAll"
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { v: List<PanacheEntityBase?>? ->
//                        NamedQueryEntity.list<PanacheEntityBase>(
//                            "#NamedQueryEntity.getAll"
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { v: List<PanacheEntityBase?>? ->
//                        NamedQueryWith2QueriesEntity.list<PanacheEntityBase>(
//                            "#NamedQueryWith2QueriesEntity.getAll1"
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { v: List<PanacheEntityBase?>? ->
//                        NamedQueryWith2QueriesEntity.list<PanacheEntityBase>(
//                            "#NamedQueryWith2QueriesEntity.getAll2"
//                        )
//                    })
//                    .flatMap<List<PanacheEntityBase>>(Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { v: List<PanacheEntityBase?>? ->
//                        Person.find<PanacheEntityBase>(
//                            ""
//                        ).list<PanacheEntityBase>()
//                    }).flatMap<List<PanacheEntityBase>>(
//                        Function<List<PanacheEntityBase?>, Uni<out MutableList<PanacheEntityBase?>?>> { persons: List<PanacheEntityBase?> ->
//                            Assertions.assertEquals(1, persons.size)
//                            Assertions.assertEquals(person, persons[0])
//                            Person.find<PanacheEntityBase>(
//                                null
//                            )
//                                .list<PanacheEntityBase>()
//                        })
//                    .flatMap<Any>(Function<List<PanacheEntityBase?>, Uni<*>> { persons: List<PanacheEntityBase?> ->
//                        Assertions.assertEquals(1, persons.size)
//                        Assertions.assertEquals(person, persons[0])
//                        Person.findById<PanacheEntityBase>(
//                            person.id
//                        )
//                    })
//                    .flatMap<Any>(Function<Any, Uni<*>> { byId: Any ->
//                        Assertions.assertEquals(person, byId)
//                        Assertions.assertEquals("Person<" + person.id + ">", byId.toString())
//                        Person.findById<PanacheEntityBase>(
//                            person.id,
//                            LockModeType.PESSIMISTIC_READ
//                        )
//                    })
//                    .flatMap<Void>(Function<Any, Uni<out Void>> { byId: Any ->
//                        Assertions.assertEquals(person, byId)
//                        Assertions.assertEquals("Person<" + person.id + ">", byId.toString())
//                        person.delete()
//                    })
//                    .flatMap<Long>(Function<Void, Uni<out Long>> { v: Void? -> Person.count() })
//                    .flatMap<Person>(Function<Long, Uni<out Person?>> { count: Long? ->
//                        Assertions.assertEquals(0, count)
//                        makeSavedPerson()
//                    })
//            })
//            .flatMap<Person>(Function<Person, Uni<out Person?>> { person: Person? ->
//                Person.count()
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        Person.delete("name = ?1", "emmanuel")
//                    })
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(0, count)
//                        Dog.delete("owner = ?1", person)
//                    })
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        Person.delete("name", "stef")
//                    })
//                    .flatMap<Person>(Function<Long, Uni<out Person?>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        makeSavedPerson()
//                    })
//            })
//            .flatMap<Person>(Function<Person, Uni<out Person?>> { person: Person? ->
//                Dog.delete(
//                    "owner = :owner",
//                    Parameters.with("owner", person).map()
//                )
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        Person.delete("name", "stef")
//                    })
//                    .flatMap<Person>(Function<Long, Uni<out Person?>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        makeSavedPerson()
//                    })
//            })
//            .flatMap<Person>(Function<Person, Uni<out Person?>> { person: Person? ->
//                Dog.delete(
//                    "owner = :owner",
//                    Parameters.with("owner", person)
//                )
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        Person.delete("name", "stef")
//                    })
//                    .flatMap<Person>(Function<Long, Uni<out Person?>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        makeSavedPerson()
//                    })
//            })
//            .flatMap<Person>(Function<Person, Uni<out Person?>> { person: Person? ->
//                Dog.delete(
//                    "FROM Dog WHERE owner = :owner",
//                    Parameters.with("owner", person)
//                )
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        Person.delete("FROM Person2 WHERE name = ?1", "stef")
//                    })
//                    .flatMap<Person>(Function<Long, Uni<out Person?>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        makeSavedPerson()
//                    })
//            })
//            .flatMap<Any>(Function<Person, Uni<*>> { person: Person? ->
//                Dog.delete(
//                    "DELETE FROM Dog WHERE owner = :owner",
//                    Parameters.with("owner", person)
//                )
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        Person.delete("DELETE FROM Person2 WHERE name = ?1", "stef")
//                    }).map<Any>(Function<Long, Any?> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        null
//                    })
//            })
//            .flatMap<Long>(Function<Any, Uni<out Long>> { v: Any? -> Person.deleteAll() })
//            .flatMap<Person>(Function<Long, Uni<out Person?>> { count: Long? ->
//                Assertions.assertEquals(0, count)
//                makeSavedPerson()
//            })
//            .flatMap<Any>(Function<Person, Uni<*>> { person: Person? ->
//                Dog.deleteAll()
//                    .flatMap<Long>(Function<Long, Uni<out Long>> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        Person.deleteAll()
//                    }).map<Any>(Function<Long, Any?> { count: Long? ->
//                        Assertions.assertEquals(1, count)
//                        null
//                    })
//            })
//            .flatMap<Void>(Function<Any, Uni<out Void>> { v: Any? ->
//                testPersist(
//                    TestEndpoint.PersistTest.Iterable
//                )
//            })
//            .flatMap<Void>(Function<Void, Uni<out Void>> { v: Void? ->
//                testPersist(
//                    TestEndpoint.PersistTest.Stream
//                )
//            })
//            .flatMap<Void>(Function<Void, Uni<out Void>> { v: Void? ->
//                testPersist(
//                    TestEndpoint.PersistTest.Variadic
//                )
//            })
//            .flatMap<Long>(Function<Void, Uni<out Long>> { v: Void? -> Person.deleteAll() })
//            .flatMap<Void>(Function<Long, Uni<out Void>> { count: Long? ->
//                Assertions.assertEquals(6, count)
//                testSorting()
//            }) // paging
//            .flatMap<Person>(Function<Void, Uni<out Person?>> { v: Void? ->
//                makeSavedPerson(
//                    "0"
//                )
//            })
//            .flatMap<Person>(Function<Person, Uni<out Person?>> { v: Person? ->
//                makeSavedPerson(
//                    "1"
//                )
//            })
//            .flatMap<Person>(Function<Person, Uni<out Person?>> { v: Person? ->
//                makeSavedPerson(
//                    "2"
//                )
//            })
//            .flatMap<Person>(Function<Person, Uni<out Person?>> { v: Person? ->
//                makeSavedPerson(
//                    "3"
//                )
//            })
//            .flatMap<Person>(Function<Person, Uni<out Person?>> { v: Person? ->
//                makeSavedPerson(
//                    "4"
//                )
//            })
//            .flatMap<Person>(Function<Person, Uni<out Person?>> { v: Person? ->
//                makeSavedPerson(
//                    "5"
//                )
//            })
//            .flatMap<Person>(Function<Person, Uni<out Person?>> { v: Person? ->
//                makeSavedPerson(
//                    "6"
//                )
//            })
//            .flatMap<Void>(Function<Person, Uni<out Void>> { v: Person? ->
//                testPaging(
//                    Person.findAll<Person>()
//                )
//            })
//            .flatMap<Void>(Function<Void, Uni<out Void>> { v: Void? ->
//                testPaging(
//                    Person.find<Person>(
//                        "ORDER BY name"
//                    )
//                )
//            }) // range
//            .flatMap<Void>(Function<Void, Uni<out Void>> { v: Void? -> testRange(Person.findAll<Person>()) })
//            .flatMap<Void>(Function<Void, Uni<out Void>> { v: Void? ->
//                testRange(
//                    Person.find<Person>(
//                        "ORDER BY name"
//                    )
//                )
//            })
//            .flatMap<Void>(Function<Void, Uni<out Void>> { v: Void? ->
//                assertThrows(
//                    NonUniqueResultException::class.java,
//                    Supplier<Uni<*>> {
//                        Person.findAll<PanacheEntityBase>()
//                            .singleResult<PanacheEntityBase>()
//                    },
//                    "singleResult should have thrown"
//                )
//            })
//            .flatMap<Any>(Function<Void, Uni<*>> { v: Void? ->
//                Person.findAll<PanacheEntityBase>()
//                    .firstResult<PanacheEntityBase>()
//            })
//            .flatMap<Long>(Function<Any, Uni<out Long>> { person: Any? ->
//                Assertions.assertNotNull(person)
//                Person.deleteAll()
//            }).flatMap<Void>(Function<Long, Uni<out Void>> { count: Long? ->
//                Assertions.assertEquals(7, count)
//                testUpdate()
//            }).flatMap<Boolean>(Function<Void, Uni<out Boolean>> { v: Void? ->
//                //delete by id
//                val toRemove: Person = Person()
//                toRemove.name = "testDeleteById"
//                toRemove.uniqueName = "testDeleteByIdUnique"
//                toRemove.persist<PanacheEntityBase>()
//                    .flatMap<Boolean>(Function<PanacheEntityBase, Uni<out Boolean>> { v2: PanacheEntityBase? ->
//                        Person.deleteById(
//                            toRemove.id
//                        )
//                    })
//            }).flatMap<Boolean>(Function<Boolean, Uni<out Boolean>> { deleted: Boolean? ->
//                Assertions.assertTrue(deleted!!)
//                Person.deleteById(666L) //not existing
//            }).flatMap<Any>(Function<Boolean, Uni<*>> { deleted: Boolean? ->
//                Assertions.assertFalse(deleted!!)
//
//                // persistAndFlush
//                val person1: Person = Person()
//                person1.name = "testFLush1"
//                person1.uniqueName = "unique"
//                person1.persist()
//            })
            .flatMap( { v: Any? -> Person.deleteAll() })
            .map { "OK" }
    }

    private fun makeSavedPerson(suffix: String): Uni<Person> {
        val person = Person().apply {
            name = "stef$suffix"
            status = Status.LIVING
            address = Address("stef street")
        }

        return person.address!!.persist<Address>()
            .flatMap { person.persist() }
    }

    private fun makeSavedPerson(): Uni<Person> {
        return makeSavedPerson("").flatMap {
            val dog = Dog("octave", "dalmatian")
            dog.owner = it
            it.dogs.add(dog)
            dog.persist<Dog>()
                .map { d -> d.owner }
        }
    }

    private fun <T> collect(stream: Multi<T>): Uni<List<T>> {
        return stream.collect().asList()
    }

    private fun assertThrows(
        exceptionClass: Class<out Throwable>,
        f: Supplier<Uni<*>>,
        message: String
    ): Uni<Void?>? {
        System.err.println("Asserting $message hoping to get a $exceptionClass")
        val uni: Uni<*> = try {
            f.get()
        } catch (t: Throwable) {
            Uni.createFrom().failure<Any>(t)
        }
        return uni
            .onItemOrFailure()
            .invoke { r: Any, t: Throwable -> System.err.println("Got back val: $r and exception $t") }
            .onItem().invoke { _: Any -> Assertions.fail<Any>(message) }
            .onFailure(exceptionClass)
            .recoverWithItem { null }
            .map { null }
    }
}