/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.example.panache;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.junit.jupiter.api.Assertions;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.jpa.PanacheQuery;

/**
 * Various tests covering Panache functionality. All tests should work in both standard JVM and SubstrateVM.
 */
@Path("test")
public class TestEndpoint {

    @GET
    @Path("model")
    @Transactional
    public String testModel() {
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

        Person byId = Person.findById(person.id);
        Assertions.assertEquals(person, byId);

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

        try {
            Person.findAll().singleResult();
            Assertions.fail("singleResult should have thrown");
        } catch (NonUniqueResultException x) {
        }

        Assertions.assertNotNull(Person.findAll().firstResult());

        Assertions.assertEquals(7, Person.deleteAll());

        return "OK";
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
        List<Person> order1 = Arrays.asList(person3, person1, person2);

        List<Person> list = Person.findAll(sort1).list();
        Assertions.assertEquals(order1, list);

        list = Person.listAll(sort1);
        Assertions.assertEquals(order1, list);

        list = Person.<Person> streamAll(sort1).collect(Collectors.toList());
        Assertions.assertEquals(order1, list);

        Sort sort2 = Sort.descending("name", "status");
        List<Person> order2 = Arrays.asList(person2, person1);

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
        dog.persist();
        person.dogs.add(dog);

        return person;
    }

    private void testPersist(PersistTest persistTest) {
        Person person1 = new Person();
        person1.name = "stef1";
        Person person2 = new Person();
        person2.name = "stef2";
        Assertions.assertFalse(person1.isPersistent());
        Assertions.assertFalse(person2.isPersistent());
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
        Assertions.assertTrue(person1.isPersistent());
        Assertions.assertTrue(person2.isPersistent());
    }

    @Inject
    PersonRepository personDao;
    @Inject
    DogDao dogDao;
    @Inject
    AddressDao addressDao;

    @GET
    @Path("model-dao")
    @Transactional
    public String testModelDao() {
        List<Person> persons = personDao.findAll().list();
        Assertions.assertEquals(0, persons.size());

        Stream<Person> personStream = personDao.findAll().stream();
        Assertions.assertEquals(0, personStream.count());

        try {
            personDao.findAll().singleResult();
            Assertions.fail("singleResult should have thrown");
        } catch (NoResultException x) {
        }

        Assertions.assertNull(personDao.findAll().firstResult());

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

        persons = personDao.find("name = ?1", "stef").list();
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

        Person byId = personDao.findById(person.id);
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

        try {
            personDao.findAll().singleResult();
            Assertions.fail("singleResult should have thrown");
        } catch (NonUniqueResultException x) {
        }

        Assertions.assertNotNull(personDao.findAll().firstResult());

        Assertions.assertEquals(7, personDao.deleteAll());

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
        List<Person> order1 = Arrays.asList(person3, person1, person2);

        List<Person> list = personDao.findAll(sort1).list();
        Assertions.assertEquals(order1, list);

        list = personDao.listAll(sort1);
        Assertions.assertEquals(order1, list);

        list = personDao.streamAll(sort1).collect(Collectors.toList());
        Assertions.assertEquals(order1, list);

        Sort sort2 = Sort.descending("name", "status");
        List<Person> order2 = Arrays.asList(person2, person1);

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
        Iterable, Variadic, Stream;
    }

    private void testPersistDao(PersistTest persistTest) {
        Person person1 = new Person();
        person1.name = "stef1";
        Person person2 = new Person();
        person2.name = "stef2";
        Assertions.assertFalse(person1.isPersistent());
        Assertions.assertFalse(person2.isPersistent());
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
        Assertions.assertTrue(person1.isPersistent());
        Assertions.assertTrue(person2.isPersistent());
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
        dogDao.persist(dog);
        person.dogs.add(dog);

        return person;
    }

    private void testPaging(PanacheQuery<Person> query) {
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
        Assertions.assertTrue(query.hasNextPage());
        Assertions.assertFalse(query.hasPreviousPage());

        persons = query.nextPage().list();
        Assertions.assertEquals(1, query.page().index);
        Assertions.assertEquals(3, query.page().size);
        Assertions.assertEquals(3, persons.size());
        Assertions.assertEquals("stef3", persons.get(0).name);
        Assertions.assertEquals("stef4", persons.get(1).name);
        Assertions.assertEquals("stef5", persons.get(2).name);
        Assertions.assertTrue(query.hasNextPage());
        Assertions.assertTrue(query.hasPreviousPage());

        persons = query.nextPage().list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals("stef6", persons.get(0).name);
        Assertions.assertFalse(query.hasNextPage());
        Assertions.assertTrue(query.hasPreviousPage());

        persons = query.nextPage().list();
        Assertions.assertEquals(0, persons.size());

        Assertions.assertEquals(7, query.count());
        Assertions.assertEquals(3, query.pageCount());
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
}
