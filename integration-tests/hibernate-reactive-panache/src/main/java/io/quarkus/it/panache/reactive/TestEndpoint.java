package io.quarkus.it.panache.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.junit.jupiter.api.Assertions;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.exception.PanacheQueryException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Various tests covering Panache functionality. All tests should work in both standard JVM and in native mode.
 */
@Path("test")
public class TestEndpoint {

    // fake unused injection point to force ArC to not remove this otherwise I can't mock it in the tests
    @Inject
    MockablePersonRepository mockablePersonRepository;

    @ReactiveTransactional
    @GET
    @Path("model")
    public Uni<String> testModel() {
        return Person.findAll().list()
                .flatMap(persons -> {
                    Assertions.assertEquals(0, persons.size());

                    return Person.listAll();
                }).flatMap(persons -> {
                    Assertions.assertEquals(0, persons.size());

                    return collect(Person.findAll().stream());
                }).flatMap(personStream -> {
                    Assertions.assertEquals(0, personStream.size());

                    return collect(Person.streamAll());
                }).flatMap(personStream -> {

                    Assertions.assertEquals(0, personStream.size());
                    return assertThrows(NoResultException.class, () -> Person.findAll().singleResult(),
                            "singleResult should have thrown");
                }).flatMap(v -> Person.findAll().firstResult())
                .flatMap(result -> {
                    Assertions.assertNull(result);

                    return makeSavedPerson();
                }).flatMap(person -> {
                    Assertions.assertNotNull(person.id);

                    return Person.count()
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);
                                return Person.count("name = ?1", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);
                                return Person.count("name = :name", Parameters.with("name", "stef").map());
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);
                                return Person.count("name = :name", Parameters.with("name", "stef"));
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);
                                return Person.count("name", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);
                                return Dog.count();
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);
                                // FIXME: fetch
                                Assertions.assertEquals(1, person.dogs.size());

                                return Person.findAll().list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return Person.listAll();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(Person.findAll().stream());
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(Person.streamAll());
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return Person.findAll().firstResult();
                            }).flatMap(personResult -> {
                                Assertions.assertEquals(person, personResult);

                                return Person.findAll().singleResult();
                            }).flatMap(personResult -> {
                                Assertions.assertEquals(person, personResult);

                                return Person.find("name = ?1", "stef").list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                // full form
                                return Person.find("FROM Person2 WHERE name = ?1", "stef").list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return Person.find("name = ?1", "stef").withLock(LockModeType.PESSIMISTIC_READ).list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                // FIXME: not supported yet
                                //                                // next calls to this query will be cached
                                //                                return Person.find("name = ?1", "stef").withHint(QueryHints.HINT_CACHEABLE, "true").list();
                                //                            }).flatMap(persons -> {
                                //                                Assertions.assertEquals(1, persons.size());
                                //                                Assertions.assertEquals(person, persons.get(0));

                                return Person.list("name = ?1", "stef");
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return Person.find("name = :name", Parameters.with("name", "stef").map()).list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return Person.find("name = :name", Parameters.with("name", "stef")).list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return Person.list("name = :name", Parameters.with("name", "stef").map());
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return Person.find("name = :name", Parameters.with("name", "stef").map()).list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return Person.list("name = :name", Parameters.with("name", "stef"));
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return Person.find("name", "stef").list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(Person.find("name = ?1", "stef").stream());
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(Person.stream("name = ?1", "stef"));
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(Person.stream("name = :name", Parameters.with("name", "stef").map()));
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(Person.stream("name = :name", Parameters.with("name", "stef")));
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(Person.find("name", "stef").stream());
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return Person.find("name", "stef").firstResult();
                            }).flatMap(personResult -> {
                                Assertions.assertEquals(person, personResult);

                                return Person.find("name", "stef").singleResult();
                            }).flatMap(personResult -> {
                                Assertions.assertEquals(person, personResult);

                                return Person.find("name", "stef").singleResult();
                            }).flatMap(personResult -> {
                                Assertions.assertEquals(person, personResult);

                                //named query
                                return Person.list("#Person.getByName", Parameters.with("name", "stef"));
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return assertThrows(PanacheQueryException.class,
                                        () -> Person.find("#Person.namedQueryNotFound").list(),
                                        "singleResult should have thrown");
                            }).flatMap(v -> NamedQueryEntity.list("#NamedQueryMappedSuperClass.getAll"))
                            .flatMap(v -> NamedQueryEntity.list("#NamedQueryEntity.getAll"))
                            .flatMap(v -> NamedQueryWith2QueriesEntity.list("#NamedQueryWith2QueriesEntity.getAll1"))
                            .flatMap(v -> NamedQueryWith2QueriesEntity.list("#NamedQueryWith2QueriesEntity.getAll2"))
                            .flatMap(v -> {
                                //empty query
                                return Person.find("").list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return Person.find(null).list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return Person.findById(person.id);
                            }).flatMap(byId -> {
                                Assertions.assertEquals(person, byId);
                                Assertions.assertEquals("Person<" + person.id + ">", byId.toString());

                                return Person.findById(person.id, LockModeType.PESSIMISTIC_READ);
                            }).flatMap(byId -> {
                                Assertions.assertEquals(person, byId);
                                Assertions.assertEquals("Person<" + person.id + ">", byId.toString());

                                return person.delete();
                            }).flatMap(v -> Person.count())
                            .flatMap(count -> {
                                Assertions.assertEquals(0, count);

                                return makeSavedPerson();
                            });
                }).flatMap(person -> {

                    return Person.count()
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return Person.delete("name = ?1", "emmanuel");
                            }).flatMap(count -> {
                                Assertions.assertEquals(0, count);

                                return Dog.delete("owner = ?1", person);
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return Person.delete("name", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return makeSavedPerson();
                            });
                }).flatMap(person -> {

                    return Dog.delete("owner = :owner", Parameters.with("owner", person).map())
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return Person.delete("name", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return makeSavedPerson();
                            });
                }).flatMap(person -> {

                    return Dog.delete("owner = :owner", Parameters.with("owner", person))
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return Person.delete("name", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return makeSavedPerson();
                            });
                }).flatMap(person -> {

                    // full form
                    return Dog.delete("FROM Dog WHERE owner = :owner", Parameters.with("owner", person))
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return Person.delete("FROM Person2 WHERE name = ?1", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return makeSavedPerson();
                            });
                }).flatMap(person -> {

                    // full form
                    return Dog.delete("DELETE FROM Dog WHERE owner = :owner", Parameters.with("owner", person))
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return Person.delete("DELETE FROM Person2 WHERE name = ?1", "stef");
                            }).map(count -> {
                                Assertions.assertEquals(1, count);

                                return null;
                            });
                })
                .flatMap(v -> Person.deleteAll())
                .flatMap(count -> {
                    Assertions.assertEquals(0, count);

                    return makeSavedPerson();
                }).flatMap(person -> {

                    return Dog.deleteAll()
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return Person.deleteAll();
                            }).map(count -> {
                                Assertions.assertEquals(1, count);

                                return null;
                            });
                })
                .flatMap(v -> testPersist(PersistTest.Iterable))
                .flatMap(v -> testPersist(PersistTest.Stream))
                .flatMap(v -> testPersist(PersistTest.Variadic))
                .flatMap(v -> Person.deleteAll())
                .flatMap(count -> {
                    Assertions.assertEquals(6, count);

                    return testSorting();
                })
                // paging
                .flatMap(v -> makeSavedPerson("0"))
                .flatMap(v -> makeSavedPerson("1"))
                .flatMap(v -> makeSavedPerson("2"))
                .flatMap(v -> makeSavedPerson("3"))
                .flatMap(v -> makeSavedPerson("4"))
                .flatMap(v -> makeSavedPerson("5"))
                .flatMap(v -> makeSavedPerson("6"))
                .flatMap(v -> testPaging(Person.findAll()))
                .flatMap(v -> testPaging(Person.find("ORDER BY name")))
                // range
                .flatMap(v -> testRange(Person.findAll()))
                .flatMap(v -> testRange(Person.find("ORDER BY name")))
                .flatMap(v -> assertThrows(NonUniqueResultException.class,
                        () -> Person.findAll().singleResult(),
                        "singleResult should have thrown"))
                .flatMap(v -> Person.findAll().firstResult())
                .flatMap(person -> {
                    Assertions.assertNotNull(person);

                    return Person.deleteAll();
                }).flatMap(count -> {
                    Assertions.assertEquals(7, count);

                    return testUpdate();
                }).flatMap(v -> {
                    //delete by id
                    Person toRemove = new Person();
                    toRemove.name = "testDeleteById";
                    toRemove.uniqueName = "testDeleteByIdUnique";
                    return toRemove.persist().flatMap(v2 -> Person.deleteById(toRemove.id));
                }).flatMap(deleted -> {
                    assertTrue(deleted);

                    return Person.deleteById(666L); //not existing
                }).flatMap(deleted -> {
                    assertFalse(deleted);

                    // persistAndFlush
                    Person person1 = new Person();
                    person1.name = "testFLush1";
                    person1.uniqueName = "unique";
                    return person1.persist();
                    // FIXME: https://github.com/hibernate/hibernate-reactive/issues/281
                    //                }).flatMap(v -> {
                    //                    Person person2 = new Person();
                    //                    person2.name = "testFLush2";
                    //                    person2.uniqueName = "unique";
                    //
                    //                    // FIXME should be PersistenceException see https://github.com/hibernate/hibernate-reactive/issues/280
                    //                    return assertThrows(PgException.class,
                    //                            () -> person2.persistAndFlush(),
                    //                            "Should have failed");
                }).flatMap(v -> Person.deleteAll())
                .map(v -> "OK");
    }

    private <T> Uni<List<T>> collect(Multi<T> stream) {
        return stream.collect().asList();
    }

    private Uni<Void> testUpdate() {
        return makeSavedPerson("p1")
                .flatMap(v -> makeSavedPerson("p2"))
                .flatMap(v -> Person.update("update from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1"))
                .flatMap(updateByIndexParameter -> {
                    Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

                    return Person.update("update from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                            Parameters.with("pName", "stefp2").map());
                }).flatMap(updateByNamedParameter -> {
                    Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

                    return Person.deleteAll();
                }).flatMap(count -> {
                    Assertions.assertEquals(2, count);

                    return makeSavedPerson("p1");
                }).flatMap(v -> makeSavedPerson("p2"))
                .flatMap(v -> Person.update("from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1"))
                .flatMap(updateByIndexParameter -> {
                    Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

                    return Person.update("from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                            Parameters.with("pName", "stefp2").map());

                }).flatMap(updateByNamedParameter -> {
                    Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

                    return Person.deleteAll();
                }).flatMap(count -> {
                    Assertions.assertEquals(2, count);

                    return makeSavedPerson("p1");
                }).flatMap(v -> makeSavedPerson("p2"))
                .flatMap(v -> Person.update("set name = 'stefNEW' where name = ?1", "stefp1"))
                .flatMap(updateByIndexParameter -> {
                    Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

                    return Person.update("set name = 'stefNEW' where name = :pName",
                            Parameters.with("pName", "stefp2").map());
                }).flatMap(updateByNamedParameter -> {
                    Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

                    return Person.deleteAll();
                }).flatMap(count -> {
                    Assertions.assertEquals(2, count);

                    return makeSavedPerson("p1");
                }).flatMap(v -> makeSavedPerson("p2"))
                .flatMap(v -> Person.update("name = 'stefNEW' where name = ?1", "stefp1"))
                .flatMap(updateByIndexParameter -> {
                    Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

                    return Person.update("name = 'stefNEW' where name = :pName",
                            Parameters.with("pName", "stefp2").map());
                }).flatMap(updateByNamedParameter -> {
                    Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

                    return Person.deleteAll();
                }).flatMap(count -> {
                    Assertions.assertEquals(2, count);

                    return makeSavedPerson("p1");
                }).flatMap(v -> makeSavedPerson("p2"))
                .flatMap(v -> Person.update("name = 'stefNEW' where name = ?1", "stefp1"))
                .flatMap(updateByIndexParameter -> {
                    Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

                    return Person.update("name = 'stefNEW' where name = :pName",
                            Parameters.with("pName", "stefp2"));
                }).flatMap(updateByNamedParameter -> {
                    Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

                    return Person.deleteAll();
                }).flatMap(count -> {
                    Assertions.assertEquals(2, count);

                    return assertThrows(PanacheQueryException.class, () -> Person.update(null),
                            "PanacheQueryException should have thrown");
                }).flatMap(v -> assertThrows(PanacheQueryException.class, () -> Person.update(" "),
                        "PanacheQueryException should have thrown"));
    }

    private Uni<Void> testUpdateDao() {
        return makeSavedPersonDao("p1")
                .flatMap(v -> makeSavedPersonDao("p2"))
                .flatMap(v -> personDao.update("update from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1"))
                .flatMap(updateByIndexParameter -> {
                    Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

                    return personDao.update("update from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                            Parameters.with("pName", "stefp2").map());
                }).flatMap(updateByNamedParameter -> {
                    Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

                    return personDao.deleteAll();
                }).flatMap(count -> {
                    Assertions.assertEquals(2, count);

                    return makeSavedPersonDao("p1");
                }).flatMap(v -> makeSavedPersonDao("p2"))
                .flatMap(v -> personDao.update("from Person2 p set p.name = 'stefNEW' where p.name = ?1", "stefp1"))
                .flatMap(updateByIndexParameter -> {
                    Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

                    return personDao.update("from Person2 p set p.name = 'stefNEW' where p.name = :pName",
                            Parameters.with("pName", "stefp2").map());

                }).flatMap(updateByNamedParameter -> {
                    Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

                    return personDao.deleteAll();
                }).flatMap(count -> {
                    Assertions.assertEquals(2, count);

                    return makeSavedPersonDao("p1");
                }).flatMap(v -> makeSavedPersonDao("p2"))
                .flatMap(v -> personDao.update("set name = 'stefNEW' where name = ?1", "stefp1"))
                .flatMap(updateByIndexParameter -> {
                    Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

                    return personDao.update("set name = 'stefNEW' where name = :pName",
                            Parameters.with("pName", "stefp2").map());
                }).flatMap(updateByNamedParameter -> {
                    Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

                    return personDao.deleteAll();
                }).flatMap(count -> {
                    Assertions.assertEquals(2, count);

                    return makeSavedPersonDao("p1");
                }).flatMap(v -> makeSavedPersonDao("p2"))
                .flatMap(v -> personDao.update("name = 'stefNEW' where name = ?1", "stefp1"))
                .flatMap(updateByIndexParameter -> {
                    Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

                    return personDao.update("name = 'stefNEW' where name = :pName",
                            Parameters.with("pName", "stefp2").map());
                }).flatMap(updateByNamedParameter -> {
                    Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

                    return personDao.deleteAll();
                }).flatMap(count -> {
                    Assertions.assertEquals(2, count);

                    return makeSavedPersonDao("p1");
                }).flatMap(v -> makeSavedPersonDao("p2"))
                .flatMap(v -> personDao.update("name = 'stefNEW' where name = ?1", "stefp1"))
                .flatMap(updateByIndexParameter -> {
                    Assertions.assertEquals(1, updateByIndexParameter, "More than one Person updated");

                    return personDao.update("name = 'stefNEW' where name = :pName",
                            Parameters.with("pName", "stefp2"));
                }).flatMap(updateByNamedParameter -> {
                    Assertions.assertEquals(1, updateByNamedParameter, "More than one Person updated");

                    return personDao.deleteAll();
                }).flatMap(count -> {
                    Assertions.assertEquals(2, count);

                    return assertThrows(PanacheQueryException.class, () -> personDao.update(null),
                            "PanacheQueryException should have thrown");
                }).flatMap(v -> assertThrows(PanacheQueryException.class, () -> personDao.update(" "),
                        "PanacheQueryException should have thrown"));
    }

    private Uni<Void> assertThrows(Class<? extends Throwable> exceptionClass,
            Supplier<Uni<?>> f,
            String message) {
        System.err.println("Asserting " + message + " hoping to get a " + exceptionClass);
        Uni<?> uni;
        try {
            uni = f.get();
        } catch (Throwable t) {
            uni = Uni.createFrom().failure(t);
        }
        return uni
                .onItemOrFailure().invoke((r, t) -> {
                    System.err.println("Got back val: " + r + " and exception " + t);
                })
                .onItem().invoke(v -> Assertions.fail(message))
                .onFailure(exceptionClass)
                .recoverWithItem(() -> null)
                .map(v -> null);
    }

    private Uni<Void> testSorting() {
        Person person1 = new Person();
        person1.name = "stef";
        person1.status = Status.LIVING;

        Person person2 = new Person();
        person2.name = "stef";
        person2.status = Status.DECEASED;

        Person person3 = new Person();
        person3.name = "emmanuel";
        person3.status = Status.LIVING;

        return person1.persist()
                .flatMap(v -> person2.persist())
                .flatMap(v -> person3.persist())
                .flatMap(v -> {
                    Sort sort1 = Sort.by("name", "status");
                    List<Person> order1 = Arrays.asList(person3, person2, person1);

                    Sort sort2 = Sort.descending("name", "status");
                    List<Person> order2 = Arrays.asList(person1, person2);

                    return Person.findAll(sort1).list()
                            .flatMap(list -> {
                                Assertions.assertEquals(order1, list);

                                return Person.listAll(sort1);
                            }).flatMap(list -> {
                                Assertions.assertEquals(order1, list);

                                return collect(Person.<Person> streamAll(sort1));
                            }).flatMap(list -> {
                                Assertions.assertEquals(order1, list);

                                return Person.find("name", sort2, "stef").list();
                            }).flatMap(list -> {
                                Assertions.assertEquals(order2, list);

                                return Person.list("name", sort2, "stef");
                            }).flatMap(list -> {
                                Assertions.assertEquals(order2, list);

                                return collect(Person.<Person> stream("name", sort2, "stef"));
                            }).flatMap(list -> {
                                Assertions.assertEquals(order2, list);

                                return Person.find("name = :name", sort2, Parameters.with("name", "stef").map()).list();
                            }).flatMap(list -> {
                                Assertions.assertEquals(order2, list);

                                return Person.list("name = :name", sort2, Parameters.with("name", "stef").map());
                            }).flatMap(list -> {
                                Assertions.assertEquals(order2, list);

                                return collect(
                                        Person.<Person> stream("name = :name", sort2, Parameters.with("name", "stef").map()));
                            }).flatMap(list -> {
                                Assertions.assertEquals(order2, list);

                                return Person.find("name = :name", sort2, Parameters.with("name", "stef")).list();
                            }).flatMap(list -> {
                                Assertions.assertEquals(order2, list);

                                return Person.list("name = :name", sort2, Parameters.with("name", "stef"));
                            }).flatMap(list -> {
                                Assertions.assertEquals(order2, list);

                                return collect(Person.<Person> stream("name = :name", sort2, Parameters.with("name", "stef")));
                            });
                }).flatMap(v -> Person.deleteAll())
                .map(count -> {
                    Assertions.assertEquals(3, count);

                    return null;
                });
    }

    private Uni<Person> makeSavedPerson(String suffix) {
        Person person = new Person();
        person.name = "stef" + suffix;
        person.status = Status.LIVING;
        person.address = new Address("stef street");
        return person.address.persist()
                .flatMap(v -> person.persist());
    }

    private Uni<Person> makeSavedPersonDao(String suffix) {
        Person person = new Person();
        person.name = "stef" + suffix;
        person.status = Status.LIVING;
        person.address = new Address("stef street");
        return addressDao.persist(person.address)
                .flatMap(v -> personDao.persist(person));
    }

    private Uni<Person> makeSavedPerson() {
        Uni<Person> uni = makeSavedPerson("");

        return uni.flatMap(person -> {
            Dog dog = new Dog("octave", "dalmatian");
            dog.owner = person;
            person.dogs.add(dog);
            return dog.persist().map(d -> person);
        });
    }

    private Uni<Person> makeSavedPersonDao() {
        Uni<Person> uni = makeSavedPersonDao("");

        return uni.flatMap(person -> {
            Dog dog = new Dog("octave", "dalmatian");
            dog.owner = person;
            person.dogs.add(dog);
            return dog.persist().map(d -> person);
        });
    }

    private Uni<Void> testPersist(PersistTest persistTest) {
        Person person1 = new Person();
        person1.name = "stef1";
        Person person2 = new Person();
        person2.name = "stef2";

        assertFalse(person1.isPersistent());
        assertFalse(person2.isPersistent());
        Uni<Void> persist;
        switch (persistTest) {
            case Iterable:
                persist = Person.persist(Arrays.asList(person1, person2));
                break;
            case Stream:
                persist = Person.persist(Stream.of(person1, person2));
                break;
            case Variadic:
                persist = Person.persist(person1, person2);
                break;
            default:
                throw new RuntimeException("Ouch");
        }
        return persist.map(v -> {
            assertTrue(person1.isPersistent());
            assertTrue(person2.isPersistent());
            return null;
        });
    }

    private Uni<Void> testPersistDao(PersistTest persistTest) {
        Person person1 = new Person();
        person1.name = "stef1";
        Person person2 = new Person();
        person2.name = "stef2";

        assertFalse(personDao.isPersistent(person1));
        assertFalse(personDao.isPersistent(person2));
        Uni<Void> persist;
        switch (persistTest) {
            case Iterable:
                persist = personDao.persist(Arrays.asList(person1, person2));
                break;
            case Stream:
                persist = personDao.persist(Stream.of(person1, person2));
                break;
            case Variadic:
                persist = personDao.persist(person1, person2);
                break;
            default:
                throw new RuntimeException("Ouch");
        }
        return persist.map(v -> {
            assertTrue(personDao.isPersistent(person1));
            assertTrue(personDao.isPersistent(person2));
            return null;
        });
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

    @ReactiveTransactional
    @GET
    @Path("model-dao")
    public Uni<String> testModelDao() {
        return personDao.findAll().list()
                .flatMap(persons -> {
                    Assertions.assertEquals(0, persons.size());

                    return personDao.listAll();
                }).flatMap(persons -> {
                    Assertions.assertEquals(0, persons.size());

                    return collect(personDao.findAll().stream());
                }).flatMap(personStream -> {
                    Assertions.assertEquals(0, personStream.size());

                    return collect(personDao.streamAll());
                }).flatMap(personStream -> {

                    Assertions.assertEquals(0, personStream.size());
                    return assertThrows(NoResultException.class, () -> personDao.findAll().singleResult(),
                            "singleResult should have thrown");
                }).flatMap(v -> personDao.findAll().firstResult())
                .flatMap(result -> {
                    Assertions.assertNull(result);

                    return makeSavedPersonDao();
                }).flatMap(person -> {
                    Assertions.assertNotNull(person.id);

                    return personDao.count()
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);
                                return personDao.count("name = ?1", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);
                                return personDao.count("name = :name", Parameters.with("name", "stef").map());
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);
                                return personDao.count("name = :name", Parameters.with("name", "stef"));
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);
                                return personDao.count("name", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);
                                return dogDao.count();
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);
                                // FIXME: fetch
                                Assertions.assertEquals(1, person.dogs.size());

                                return personDao.findAll().list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.listAll();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(personDao.findAll().stream());
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(personDao.streamAll());
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.findAll().firstResult();
                            }).flatMap(personResult -> {
                                Assertions.assertEquals(person, personResult);

                                return personDao.findAll().singleResult();
                            }).flatMap(personResult -> {
                                Assertions.assertEquals(person, personResult);

                                return personDao.find("name = ?1", "stef").list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.find("name = ?1", "stef").withLock(LockModeType.PESSIMISTIC_READ).list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                // FIXME: not supported yet
                                //                                // next calls to this query will be cached
                                //                                return personDao.find("name = ?1", "stef").withHint(QueryHints.HINT_CACHEABLE, "true").list();
                                //                            }).flatMap(persons -> {
                                //                                Assertions.assertEquals(1, persons.size());
                                //                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.list("name = ?1", "stef");
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.find("name = :name", Parameters.with("name", "stef").map()).list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.find("name = :name", Parameters.with("name", "stef")).list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.list("name = :name", Parameters.with("name", "stef").map());
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.find("name = :name", Parameters.with("name", "stef").map()).list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.list("name = :name", Parameters.with("name", "stef"));
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.find("name", "stef").list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(personDao.find("name = ?1", "stef").stream());
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(personDao.stream("name = ?1", "stef"));
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(personDao.stream("name = :name", Parameters.with("name", "stef").map()));
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(personDao.stream("name = :name", Parameters.with("name", "stef")));
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return collect(personDao.find("name", "stef").stream());
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.find("name", "stef").firstResult();
                            }).flatMap(personResult -> {
                                Assertions.assertEquals(person, personResult);

                                return personDao.find("name", "stef").singleResult();
                            }).flatMap(personResult -> {
                                Assertions.assertEquals(person, personResult);

                                return personDao.find("name", "stef").singleResult();
                            }).flatMap(personResult -> {
                                Assertions.assertEquals(person, personResult);

                                //named query
                                return personDao.list("#Person.getByName", Parameters.with("name", "stef"));
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return assertThrows(PanacheQueryException.class,
                                        () -> personDao.find("#Person.namedQueryNotFound").list(),
                                        "singleResult should have thrown");
                            }).flatMap(v -> namedQueryRepository.list("#NamedQueryMappedSuperClass.getAll"))
                            .flatMap(v -> namedQueryRepository.list("#NamedQueryEntity.getAll"))
                            .flatMap(v -> namedQueryWith2QueriesRepository.list("#NamedQueryWith2QueriesEntity.getAll1"))
                            .flatMap(v -> namedQueryWith2QueriesRepository.list("#NamedQueryWith2QueriesEntity.getAll2"))
                            .flatMap(v -> {
                                //empty query
                                return personDao.find("").list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.find(null).list();
                            }).flatMap(persons -> {
                                Assertions.assertEquals(1, persons.size());
                                Assertions.assertEquals(person, persons.get(0));

                                return personDao.findById(person.id);
                            }).flatMap(byId -> {
                                Assertions.assertEquals(person, byId);
                                Assertions.assertEquals("Person<" + person.id + ">", byId.toString());

                                return personDao.findById(person.id, LockModeType.PESSIMISTIC_READ);
                            }).flatMap(byId -> {
                                Assertions.assertEquals(person, byId);
                                Assertions.assertEquals("Person<" + person.id + ">", byId.toString());

                                return person.delete();
                            }).flatMap(v -> personDao.count())
                            .flatMap(count -> {
                                Assertions.assertEquals(0, count);

                                return makeSavedPersonDao();
                            })

                            .flatMap(v -> Person.count("#Person.countAll")
                                    .flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Person.count("#Person.countByName", Parameters.with("name", "stef").map());
                                    }).flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Person.count("#Person.countByName", Parameters.with("name", "stef"));
                                    }).flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Person.count("#Person.countByName.ordinal", "stef");
                                    }).flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Uni.createFrom().voidItem();
                                    }))
                            .flatMap(voidUni -> Person.update("#Person.updateAllNames", Parameters.with("name", "stef2").map())
                                    .flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Person.find("#Person.getByName", Parameters.with("name", "stef2")).list();
                                    }).flatMap(persons -> {
                                        Assertions.assertEquals(1, persons.size());
                                        return Person.update("#Person.updateAllNames", Parameters.with("name", "stef3"));
                                    }).flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Person.find("#Person.getByName", Parameters.with("name", "stef3")).list();
                                    }).flatMap(persons -> {
                                        Assertions.assertEquals(1, persons.size());
                                        return Person.update("#Person.updateNameById",
                                                Parameters.with("name", "stef2").and("id", ((Person) persons.get(0)).id).map());
                                    }).flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Person.find("#Person.getByName", Parameters.with("name", "stef2")).list();
                                    }).flatMap(persons -> {
                                        Assertions.assertEquals(1, persons.size());
                                        return Person.update("#Person.updateNameById",
                                                Parameters.with("name", "stef3").and("id", ((Person) persons.get(0)).id));
                                    }).flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Person.find("#Person.getByName", Parameters.with("name", "stef3")).list();
                                    }).flatMap(persons -> {
                                        Assertions.assertEquals(1, persons.size());
                                        return Person.update("#Person.updateNameById.ordinal", "stef",
                                                ((Person) persons.get(0)).id);
                                    }).flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Person.find("#Person.getByName", Parameters.with("name", "stef")).list();
                                    }).flatMap(persons -> {
                                        Assertions.assertEquals(1, persons.size());
                                        return Uni.createFrom().voidItem();
                                    }))
                            .flatMap(voidUni -> Dog.deleteAll()
                                    .flatMap(v -> Person.delete("#Person.deleteAll"))
                                    .flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Person.find("").list();
                                    })
                                    .flatMap(persons -> {
                                        Assertions.assertEquals(0, persons.size());
                                        return Uni.createFrom().voidItem();
                                    }))
                            .flatMap(voidUni -> makeSavedPerson().flatMap(personToDelete -> Dog.deleteAll()
                                    .flatMap(v -> Person.find("").list())
                                    .flatMap(persons -> {
                                        Assertions.assertEquals(1, persons.size());
                                        return Person.delete("#Person.deleteById",
                                                Parameters.with("id", personToDelete.id).map());
                                    })
                                    .flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Person.find("").list();
                                    })
                                    .flatMap(persons -> {
                                        Assertions.assertEquals(0, persons.size());
                                        return Uni.createFrom().voidItem();
                                    })))
                            .flatMap(voidUni -> makeSavedPerson().flatMap(personToDelete -> Dog.deleteAll()
                                    .flatMap(v -> Person.find("").list())
                                    .flatMap(persons -> {
                                        Assertions.assertEquals(1, persons.size());
                                        return Person.delete("#Person.deleteById", Parameters.with("id", personToDelete.id));
                                    })
                                    .flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Person.find("").list();
                                    })
                                    .flatMap(persons -> {
                                        Assertions.assertEquals(0, persons.size());
                                        return Uni.createFrom().voidItem();
                                    })))
                            .flatMap(voidUni -> makeSavedPerson().flatMap(personToDelete -> Dog.deleteAll()
                                    .flatMap(v -> Person.find("").list())
                                    .flatMap(persons -> {
                                        Assertions.assertEquals(1, persons.size());
                                        return Person.delete("#Person.deleteById.ordinal", personToDelete.id);
                                    })
                                    .flatMap(count -> {
                                        Assertions.assertEquals(1, count);
                                        return Person.find("").list();
                                    })
                                    .flatMap(persons -> {
                                        Assertions.assertEquals(0, persons.size());
                                        return makeSavedPersonDao();
                                    })));

                }).flatMap(person -> {

                    return personDao.count()
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return personDao.delete("name = ?1", "emmanuel");
                            }).flatMap(count -> {
                                Assertions.assertEquals(0, count);

                                return dogDao.delete("owner = ?1", person);
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return personDao.delete("name", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return makeSavedPersonDao();
                            });
                }).flatMap(person -> {

                    return dogDao.delete("owner = :owner", Parameters.with("owner", person).map())
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return personDao.delete("name", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return makeSavedPersonDao();
                            });
                }).flatMap(person -> {

                    return dogDao.delete("owner = :owner", Parameters.with("owner", person))
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return personDao.delete("name", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return makeSavedPersonDao();
                            });
                }).flatMap(person -> {

                    // full form
                    return dogDao.delete("FROM Dog WHERE owner = :owner", Parameters.with("owner", person))
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return personDao.delete("FROM Person2 WHERE name = ?1", "stef");
                            }).flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return makeSavedPersonDao();
                            });
                }).flatMap(person -> {

                    return dogDao.delete("DELETE FROM Dog WHERE owner = :owner", Parameters.with("owner", person))
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return personDao.delete("DELETE FROM Person2 WHERE name = ?1", "stef");
                            }).map(count -> {
                                Assertions.assertEquals(1, count);

                                return null;
                            });
                })
                .flatMap(v -> personDao.deleteAll())
                .flatMap(count -> {
                    Assertions.assertEquals(0, count);

                    return makeSavedPersonDao();
                }).flatMap(person -> {

                    return dogDao.deleteAll()
                            .flatMap(count -> {
                                Assertions.assertEquals(1, count);

                                return personDao.deleteAll();
                            }).map(count -> {
                                Assertions.assertEquals(1, count);

                                return null;
                            });
                })
                .flatMap(v -> testPersistDao(PersistTest.Iterable))
                .flatMap(v -> testPersistDao(PersistTest.Stream))
                .flatMap(v -> testPersistDao(PersistTest.Variadic))
                .flatMap(v -> personDao.deleteAll())
                .flatMap(count -> {
                    Assertions.assertEquals(6, count);

                    return testSorting();
                })
                // paging
                .flatMap(v -> makeSavedPersonDao("0"))
                .flatMap(v -> makeSavedPersonDao("1"))
                .flatMap(v -> makeSavedPersonDao("2"))
                .flatMap(v -> makeSavedPersonDao("3"))
                .flatMap(v -> makeSavedPersonDao("4"))
                .flatMap(v -> makeSavedPersonDao("5"))
                .flatMap(v -> makeSavedPersonDao("6"))
                .flatMap(v -> testPaging(personDao.findAll()))
                .flatMap(v -> testPaging(personDao.find("ORDER BY name")))
                // range
                .flatMap(v -> testRange(personDao.findAll()))
                .flatMap(v -> testRange(personDao.find("ORDER BY name")))
                .flatMap(v -> assertThrows(NonUniqueResultException.class,
                        () -> personDao.findAll().singleResult(),
                        "singleResult should have thrown"))
                .flatMap(v -> personDao.findAll().firstResult())
                .flatMap(person -> {
                    Assertions.assertNotNull(person);

                    return personDao.deleteAll();
                }).flatMap(count -> {
                    Assertions.assertEquals(7, count);

                    return testUpdateDao();
                }).flatMap(v -> {
                    //delete by id
                    Person toRemove = new Person();
                    toRemove.name = "testDeleteById";
                    toRemove.uniqueName = "testDeleteByIdUnique";
                    return toRemove.persist().flatMap(v2 -> {
                        System.err.println("Calling DELETE for " + toRemove.id);
                        return personDao.deleteById(toRemove.id);
                    });
                }).flatMap(deleted -> {
                    assertTrue(deleted);

                    return personDao.deleteById(666L); //not existing
                }).flatMap(deleted -> {
                    assertFalse(deleted);

                    // persistAndFlush
                    Person person1 = new Person();
                    person1.name = "testFLush1";
                    person1.uniqueName = "unique";
                    return personDao.persistAndFlush(person1);
                    // FIXME: https://github.com/hibernate/hibernate-reactive/issues/281
                    //                }).flatMap(v -> {
                    //                    Person person2 = new Person();
                    //                    person2.name = "testFLush2";
                    //                    person2.uniqueName = "unique";
                    //
                    //                    // FIXME should be PersistenceException see https://github.com/hibernate/hibernate-reactive/issues/280
                    //                    return assertThrows(PgException.class,
                    //                            () -> personDao.persistAndFlush(person2),
                    //                            "Should have failed");
                }).flatMap(v -> {
                    System.err.println("DONE before delete ?");
                    return personDao.deleteAll();
                })
                .map(v -> {
                    System.err.println("deleted " + v);
                    return "OK";
                });
    }

    enum PersistTest {
        Iterable,
        Variadic,
        Stream;
    }

    private Uni<Void> testPaging(PanacheQuery<Person> query) {
        // No paging allowed until a page is setup
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.firstPage(),
                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.previousPage(),
                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.nextPage(),
                "UnsupportedOperationException should have thrown");
        //        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.lastPage(),
        //                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.hasNextPage(),
                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.hasPreviousPage(),
                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.page(),
                "UnsupportedOperationException should have thrown");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.pageCount(),
                "UnsupportedOperationException should have thrown");

        // ints
        return query.page(0, 3).list()
                .flatMap(persons -> {
                    Assertions.assertEquals(3, persons.size());
                    Assertions.assertEquals("stef0", persons.get(0).name);
                    Assertions.assertEquals("stef1", persons.get(1).name);
                    Assertions.assertEquals("stef2", persons.get(2).name);

                    return query.page(1, 3).list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(3, persons.size());
                    Assertions.assertEquals("stef3", persons.get(0).name);
                    Assertions.assertEquals("stef4", persons.get(1).name);
                    Assertions.assertEquals("stef5", persons.get(2).name);

                    return query.page(2, 3).list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(1, persons.size());
                    Assertions.assertEquals("stef6", persons.get(0).name);

                    return query.page(2, 4).list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(0, persons.size());

                    // page
                    Page page = new Page(3);
                    return query.page(page).list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(3, persons.size());
                    Assertions.assertEquals("stef0", persons.get(0).name);
                    Assertions.assertEquals("stef1", persons.get(1).name);
                    Assertions.assertEquals("stef2", persons.get(2).name);

                    Page page = new Page(1, 3);
                    return query.page(page).list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(3, persons.size());
                    Assertions.assertEquals("stef3", persons.get(0).name);
                    Assertions.assertEquals("stef4", persons.get(1).name);
                    Assertions.assertEquals("stef5", persons.get(2).name);

                    Page page = new Page(2, 3);
                    return query.page(page).list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(1, persons.size());
                    Assertions.assertEquals("stef6", persons.get(0).name);

                    Page page = new Page(3, 3);
                    return query.page(page).list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(0, persons.size());

                    // query paging
                    Page page = new Page(3);
                    return query.page(page).list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(3, persons.size());
                    Assertions.assertEquals("stef0", persons.get(0).name);
                    Assertions.assertEquals("stef1", persons.get(1).name);
                    Assertions.assertEquals("stef2", persons.get(2).name);
                    return query.hasNextPage();
                }).flatMap(hasNextPage -> {
                    assertTrue(hasNextPage);
                    assertFalse(query.hasPreviousPage());

                    return query.nextPage().list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(1, query.page().index);
                    Assertions.assertEquals(3, query.page().size);
                    Assertions.assertEquals(3, persons.size());
                    Assertions.assertEquals("stef3", persons.get(0).name);
                    Assertions.assertEquals("stef4", persons.get(1).name);
                    Assertions.assertEquals("stef5", persons.get(2).name);
                    return query.hasNextPage();
                }).flatMap(hasNextPage -> {
                    assertTrue(hasNextPage);
                    assertTrue(query.hasPreviousPage());

                    return query.nextPage().list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(1, persons.size());
                    Assertions.assertEquals("stef6", persons.get(0).name);

                    return query.hasNextPage();
                }).flatMap(hasNextPage -> {
                    assertFalse(hasNextPage);
                    assertTrue(query.hasPreviousPage());

                    return query.nextPage().list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(0, persons.size());

                    return query.count();
                }).flatMap(count -> {
                    Assertions.assertEquals(7, count);

                    return query.pageCount();
                }).flatMap(count -> {
                    Assertions.assertEquals(3, count);

                    // mix page with range
                    return query.page(0, 3).range(0, 1).list();
                }).map(persons -> {
                    Assertions.assertEquals(2, persons.size());
                    Assertions.assertEquals("stef0", persons.get(0).name);
                    Assertions.assertEquals("stef1", persons.get(1).name);

                    return null;
                });
    }

    private Uni<Void> testRange(PanacheQuery<Person> query) {
        return query.range(0, 2).list()
                .flatMap(persons -> {
                    Assertions.assertEquals(3, persons.size());
                    Assertions.assertEquals("stef0", persons.get(0).name);
                    Assertions.assertEquals("stef1", persons.get(1).name);
                    Assertions.assertEquals("stef2", persons.get(2).name);

                    return query.range(3, 5).list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(3, persons.size());
                    Assertions.assertEquals("stef3", persons.get(0).name);
                    Assertions.assertEquals("stef4", persons.get(1).name);
                    Assertions.assertEquals("stef5", persons.get(2).name);

                    return query.range(6, 8).list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(1, persons.size());
                    Assertions.assertEquals("stef6", persons.get(0).name);

                    return query.range(8, 12).list();
                }).flatMap(persons -> {
                    Assertions.assertEquals(0, persons.size());

                    // mix range with page
                    Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).nextPage());
                    Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).previousPage());
                    Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).pageCount());
                    //                    Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).lastPage());
                    Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).firstPage());
                    Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).hasPreviousPage());
                    Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).hasNextPage());
                    Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).page());

                    // this is valid as we switch from range to page
                    return query.range(0, 2).page(0, 3).list();
                }).map(persons -> {
                    Assertions.assertEquals(3, persons.size());
                    Assertions.assertEquals("stef0", persons.get(0).name);
                    Assertions.assertEquals("stef1", persons.get(1).name);
                    Assertions.assertEquals("stef2", persons.get(2).name);

                    return null;
                });
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

    @ReactiveTransactional
    @GET
    @Path("model1")
    public Uni<String> testModel1() {
        return Person.count()
                .flatMap(count -> {
                    Assertions.assertEquals(0, count);

                    return makeSavedPerson("");
                }).flatMap(person -> {
                    SelfDirtinessTracker trackingPerson = (SelfDirtinessTracker) person;

                    String[] dirtyAttributes = trackingPerson.$$_hibernate_getDirtyAttributes();
                    Assertions.assertEquals(0, dirtyAttributes.length);

                    person.name = "1";

                    dirtyAttributes = trackingPerson.$$_hibernate_getDirtyAttributes();
                    Assertions.assertEquals(1, dirtyAttributes.length);

                    return Person.count();
                }).map(count -> {
                    Assertions.assertEquals(1, count);

                    return "OK";
                });
    }

    @ReactiveTransactional
    @GET
    @Path("model2")
    public Uni<String> testModel2() {
        return Person.count()
                .flatMap(count -> {
                    Assertions.assertEquals(1, count);

                    return Person.findAll().<Person> firstResult();
                }).map(person -> {
                    Assertions.assertEquals("1", person.name);

                    person.name = "2";
                    return "OK";
                });
    }

    @ReactiveTransactional
    @GET
    @Path("projection1")
    public Uni<String> testProjection() {
        return Person.count()
                .flatMap(count -> {
                    Assertions.assertEquals(1, count);

                    return Person.findAll().project(PersonName.class).<PersonName> firstResult();
                }).flatMap(person -> {
                    Assertions.assertEquals("2", person.name);

                    return Person.find("name", "2").project(PersonName.class).<PersonName> firstResult();
                }).flatMap(person -> {
                    Assertions.assertEquals("2", person.name);

                    return Person.find("name = ?1", "2").project(PersonName.class).<PersonName> firstResult();
                }).flatMap(person -> {
                    Assertions.assertEquals("2", person.name);

                    return Person.find("name = :name", Parameters.with("name", "2")).project(PersonName.class)
                            .<PersonName> firstResult();
                }).flatMap(person -> {
                    Assertions.assertEquals("2", person.name);

                    PanacheQuery<PersonName> query = Person.findAll().project(PersonName.class).page(0, 2);
                    return query.list()
                            .flatMap(results -> {
                                Assertions.assertEquals(1, results.size());

                                query.nextPage();
                                return query.list();
                            }).flatMap(results -> {
                                Assertions.assertEquals(0, results.size());

                                return Person.findAll().project(PersonName.class).count();
                            }).map(count -> {
                                Assertions.assertEquals(1, count);

                                return "OK";
                            });
                });
    }

    @ReactiveTransactional
    @GET
    @Path("projection2")
    public Uni<String> testProjection2() {
        String ownerName = "Julie";
        String catName = "Bubulle";
        CatOwner catOwner = new CatOwner(ownerName);
        return catOwner.persist()
                .chain(() -> new Cat(catName, catOwner).persist())
                .chain(() -> Cat.find("name", catName)
                        .project(CatDto.class)
                        .<CatDto> firstResult())
                .map(cat -> {
                    Assertions.assertEquals(catName, cat.name);
                    Assertions.assertEquals(ownerName, cat.ownerName);
                    return "OK";
                });
    }

    @ReactiveTransactional
    @GET
    @Path("model3")
    public Uni<String> testModel3() {
        return Person.count()
                .flatMap(count -> {
                    Assertions.assertEquals(1, count);

                    return Person.findAll().<Person> firstResult();
                })
                .flatMap(person -> {
                    Assertions.assertEquals("2", person.name);

                    return Dog.deleteAll();
                }).flatMap(v -> Person.deleteAll())
                .flatMap(v -> Address.deleteAll())
                .flatMap(v -> Person.count())
                .map(count -> {
                    Assertions.assertEquals(0, count);

                    return "OK";
                });
    }

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
    public Uni<String> testBug5274() {
        return bug5274EntityRepository.count()
                .map(v -> "OK");
    }

    @Inject
    Bug5885EntityRepository bug5885EntityRepository;

    @GET
    @Path("5885")
    public Uni<String> testBug5885() {
        return bug5885EntityRepository.findById(1L)
                .map(v -> "OK");
    }

    @ReactiveTransactional
    @GET
    @Path("composite")
    public Uni<String> testCompositeKey() {
        ObjectWithCompositeId obj = new ObjectWithCompositeId();
        obj.part1 = "part1";
        obj.part2 = "part2";
        obj.description = "description";
        return obj.persist()
                .flatMap(v -> {
                    ObjectWithCompositeId.ObjectKey key = new ObjectWithCompositeId.ObjectKey("part1", "part2");
                    return ObjectWithCompositeId.findById(key)
                            .flatMap(result -> {
                                assertNotNull(result);

                                return ObjectWithCompositeId.deleteById(key);
                            }).flatMap(deleted -> {
                                assertTrue(deleted);

                                ObjectWithCompositeId.ObjectKey notExistingKey = new ObjectWithCompositeId.ObjectKey(
                                        "notexist1",
                                        "notexist2");
                                return ObjectWithCompositeId.deleteById(key);
                            }).flatMap(deleted -> {
                                assertFalse(deleted);

                                ObjectWithEmbeddableId.ObjectKey embeddedKey = new ObjectWithEmbeddableId.ObjectKey("part1",
                                        "part2");
                                ObjectWithEmbeddableId embeddable = new ObjectWithEmbeddableId();
                                embeddable.key = embeddedKey;
                                embeddable.description = "description";
                                return embeddable.persist()
                                        .flatMap(v2 -> ObjectWithEmbeddableId.findById(embeddedKey))
                                        .flatMap(embeddableResult -> {
                                            assertNotNull(embeddableResult);

                                            return ObjectWithEmbeddableId.deleteById(embeddedKey);
                                        }).flatMap(deleted2 -> {
                                            assertTrue(deleted2);

                                            ObjectWithEmbeddableId.ObjectKey notExistingEmbeddedKey = new ObjectWithEmbeddableId.ObjectKey(
                                                    "notexist1",
                                                    "notexist2");
                                            return ObjectWithEmbeddableId.deleteById(notExistingEmbeddedKey);
                                        }).map(deleted2 -> {
                                            assertFalse(deleted2);

                                            return "OK";
                                        });
                            });
                });
    }

    @GET
    @Path("7721")
    public Uni<String> testBug7721() {
        Bug7721Entity entity = new Bug7721Entity();
        return Panache.withTransaction(() -> entity.persist()
                .flatMap(v -> entity.delete())
                .map(v -> "OK"));
    }

    @ReactiveTransactional
    @GET
    @Path("8254")
    public Uni<String> testBug8254() {
        CatOwner owner = new CatOwner("8254");
        return owner.persist()
                .flatMap(v -> new Cat(owner).persist())
                .flatMap(v -> new Cat(owner).persist())
                .flatMap(v -> new Cat(owner).persist())
                // This used to fail with an invalid query "SELECT COUNT(*) SELECT DISTINCT cat.owner FROM Cat cat WHERE cat.owner = ?1"
                // Should now result in a valid query "SELECT COUNT(DISTINCT cat.owner) FROM Cat cat WHERE cat.owner = ?1"
                .flatMap(v -> CatOwner.find("SELECT DISTINCT cat.owner FROM Cat cat WHERE cat.owner = ?1", owner).count())
                .flatMap(count -> {
                    assertEquals(1L, count);

                    // This used to fail with an invalid query "SELECT COUNT(*) SELECT cat.owner FROM Cat cat WHERE cat.owner = ?1"
                    // Should now result in a valid query "SELECT COUNT(cat.owner) FROM Cat cat WHERE cat.owner = ?1"
                    return CatOwner.find("SELECT cat.owner FROM Cat cat WHERE cat.owner = ?1", owner).count();
                }).flatMap(count -> {
                    assertEquals(3L, count);

                    // This used to fail with an invalid query "SELECT COUNT(*) SELECT cat FROM Cat cat WHERE cat.owner = ?1"
                    // Should now result in a valid query "SELECT COUNT(cat) FROM Cat cat WHERE cat.owner = ?1"
                    return Cat.find("SELECT cat FROM Cat cat WHERE cat.owner = ?1", owner).count();
                }).flatMap(count -> {
                    assertEquals(3L, count);

                    // This didn't use to fail. Make sure it still doesn't.
                    return Cat.find("FROM Cat WHERE owner = ?1", owner).count();
                }).flatMap(count -> {
                    assertEquals(3L, count);

                    return Cat.find("owner", owner).count();
                }).flatMap(count -> {
                    assertEquals(3L, count);

                    return CatOwner.find("name = ?1", "8254").count();
                }).map(count -> {
                    assertEquals(1L, count);

                    return "OK";
                });
    }

    @ReactiveTransactional
    @GET
    @Path("9025")
    public Uni<String> testBug9025() {
        Fruit apple = new Fruit("apple", "red");
        Fruit orange = new Fruit("orange", "orange");
        Fruit banana = new Fruit("banana", "yellow");

        return Fruit.persist(apple, orange, banana)
                .flatMap(v -> {
                    PanacheQuery<Fruit> query = Fruit.find(
                            "select name, color from Fruit").page(Page.ofSize(1));

                    return query.list()
                            .flatMap(v2 -> query.pageCount())
                            .map(v2 -> "OK");
                });
    }

    @ReactiveTransactional
    @GET
    @Path("9036")
    public Uni<String> testBug9036() {
        return Person.deleteAll()
                .flatMap(v -> new Person().persist())
                .flatMap(v -> {
                    Person deadPerson = new Person();
                    deadPerson.name = "Stef";
                    deadPerson.status = Status.DECEASED;
                    return deadPerson.persist();
                }).flatMap(v -> {
                    Person livePerson = new Person();
                    livePerson.name = "Stef";
                    livePerson.status = Status.LIVING;
                    return livePerson.persist();
                }).flatMap(v -> Person.count())
                .flatMap(count -> {
                    assertEquals(3, count);

                    return Person.listAll();
                }).flatMap(list -> {
                    assertEquals(3, list.size());

                    return Person.find("status", Status.LIVING).firstResult();
                }).flatMap(livePerson -> {
                    // should be filtered
                    PanacheQuery<Person> query = Person.findAll(Sort.by("id")).filter("Person.isAlive").filter("Person.hasName",
                            Parameters.with("name", "Stef"));

                    return query.count()
                            .flatMap(count -> {
                                assertEquals(1, count);

                                return query.list();
                            }).flatMap(list -> {
                                assertEquals(1, list.size());

                                assertEquals(livePerson, list.get(0));

                                return collect(query.stream());
                            }).flatMap(list -> {
                                assertEquals(1, list.size());

                                return query.firstResult();
                            }).flatMap(result -> {
                                assertEquals(livePerson, result);

                                return query.singleResult();
                            }).flatMap(result -> {
                                assertEquals(livePerson, result);

                                // these should be unaffected
                                return Person.count();
                            }).flatMap(count -> {
                                assertEquals(3, count);

                                return Person.listAll();
                            }).flatMap(list -> {
                                assertEquals(3, list.size());

                                return Person.deleteAll();
                            }).map(v -> "OK");
                });
    }
}
