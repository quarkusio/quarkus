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

package org.jboss.shamrock.example.panache;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.panache.jpa.Page;
import org.jboss.panache.jpa.Query;
import org.junit.jupiter.api.Assertions;

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

        Stream<Person> personStream = Person.findAll().stream();
        Assertions.assertEquals(0, personStream.count());

        Person person = makeSavedPerson();
        Assertions.assertNotNull(person.id);

        Assertions.assertEquals(1, Person.count());
        Assertions.assertEquals(1, Person.count("name = ?1", "stef"));
        Assertions.assertEquals(1, Person.count("name", "stef"));

        Assertions.assertEquals(1, Dog.count());
        Assertions.assertEquals(1, person.dogs.size());

        persons = Person.findAll().list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        personStream = Person.findAll().stream();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        persons = Person.find("name = ?1", "stef").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = Person.find("name", "stef").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        personStream = Person.find("name = ?1", "stef").stream();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        personStream = Person.find("name", "stef").stream();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        Person byId = Person.findById(person.id);
        Assertions.assertEquals(person, byId);

        person.delete();
        Assertions.assertEquals(0, Person.count());
        
        person = makeSavedPerson();
        Assertions.assertEquals(1, Person.count());
        Assertions.assertEquals(0, Person.delete("name = ?1", "emmanuel"));
        Assertions.assertEquals(1, Dog.delete("owner = ?1", person));
        Assertions.assertEquals(1, Person.delete("name", "stef"));

        Assertions.assertEquals(0, Person.deleteAll());

        makeSavedPerson();
        Assertions.assertEquals(1, Dog.deleteAll());
        Assertions.assertEquals(1, Person.deleteAll());

        // paging
        for(int i=0;i<7;i++) {
            makeSavedPerson(String.valueOf(i));
        }
        testPaging(Person.findAll());
        testPaging(Person.find("ORDER BY name"));
        Assertions.assertEquals(7, Person.deleteAll());

        return "OK";
    }

    private Person makeSavedPerson(String suffix) {
        Person person = new Person();
        person.name = "stef"+suffix;
        person.status = Status.LIVING;
        person.address = new Address("stef street");
        person.address.save();
        
        person.save();
        return person;
    }
    
    private Person makeSavedPerson() {
        Person person = makeSavedPerson("");

        Dog dog = new Dog("octave", "dalmatian");
        dog.owner = person;
        dog.save();
        person.dogs.add(dog);
        
        return person;
    }

    @Inject
    PersonDao personDao;
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

        Person person = makeSavedPersonDao();
        Assertions.assertNotNull(person.id);

        Assertions.assertEquals(1, personDao.count());
        Assertions.assertEquals(1, personDao.count("name = ?1", "stef"));
        Assertions.assertEquals(1, personDao.count("name", "stef"));

        Assertions.assertEquals(1, dogDao.count());
        Assertions.assertEquals(1, person.dogs.size());

        persons = personDao.findAll().list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        personStream = personDao.findAll().stream();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        persons = personDao.find("name = ?1", "stef").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        persons = personDao.find("name", "stef").list();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));

        personStream = personDao.find("name = ?1", "stef").stream();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        personStream = personDao.find("name", "stef").stream();
        Assertions.assertEquals(persons, personStream.collect(Collectors.toList()));

        Person byId = personDao.findById(person.id);
        Assertions.assertEquals(person, byId);

        personDao.delete(person);
        Assertions.assertEquals(0, personDao.count());
        
        person = makeSavedPersonDao();
        Assertions.assertEquals(1, personDao.count());
        Assertions.assertEquals(0, personDao.delete("name = ?1", "emmanuel"));
        Assertions.assertEquals(1, dogDao.delete("owner = ?1", person));
        Assertions.assertEquals(1, personDao.delete("name", "stef"));

        Assertions.assertEquals(0, personDao.deleteAll());

        makeSavedPersonDao();
        Assertions.assertEquals(1, dogDao.deleteAll());
        Assertions.assertEquals(1, personDao.deleteAll());

        // paging
        for(int i=0;i<7;i++) {
            makeSavedPersonDao(String.valueOf(i));
        }
        testPaging(personDao.findAll());
        testPaging(personDao.find("ORDER BY name"));
        Assertions.assertEquals(7, personDao.deleteAll());

        return "OK";
    }

    private Person makeSavedPersonDao(String suffix) {
        Person person = new Person();
        person.name = "stef"+suffix;
        person.status = Status.LIVING;
        person.address = new Address("stef street");
        addressDao.save(person.address);

        personDao.save(person);
        
        return person;
    }
    
    private Person makeSavedPersonDao() {
        Person person = makeSavedPersonDao("");

        Dog dog = new Dog("octave", "dalmatian");
        dog.owner = person;
        dogDao.save(dog);
        person.dogs.add(dog);
        
        return person;
    }

    private void testPaging(Query<Person> query) {
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
        }catch(NoSuchMethodException x) {}

        try {
            checkMethod(AccessorEntity.class, "setTrans2", void.class, Object.class);
            Assertions.fail("transient field should have no setter: trans2");
        }catch(NoSuchMethodException x) {}

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

    private void checkMethod(Class<?> klass, String name, Class<?> returnType, Class<?>... params) throws NoSuchMethodException, SecurityException {
        Method method = klass.getMethod(name, params);
        Assertions.assertEquals(returnType, method.getReturnType());
    }
}
