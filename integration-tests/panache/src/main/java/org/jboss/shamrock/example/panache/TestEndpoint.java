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

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
        List<Person> persons = Person.findAll();
        Assertions.assertEquals(0, persons.size());
        
        Person person = makeSavedPerson();
        Assertions.assertNotNull(person.id);

        Assertions.assertEquals(1, Person.count());
        Assertions.assertEquals(1, Person.count("name = ?1", "stef"));
        Assertions.assertEquals(1, Person.count("name", "stef"));

        Assertions.assertEquals(1, Dog.count());
        Assertions.assertEquals(1, person.dogs.size());

        persons = Person.findAll();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));
        
        persons = Person.find("name = ?1", "stef");
        persons = Person.find("name", "stef");
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));
        
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

        return "OK";
    }

    private Person makeSavedPerson() {
        Person person = new Person();
        person.name = "stef";
        person.status = Status.LIVING;
        person.address = new Address("stef street");
        person.address.save();
        
        person.save();

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
        List<Person> persons = personDao.findAll();
        Assertions.assertEquals(0, persons.size());
        
        Person person = makeSavedPersonDao();
        Assertions.assertNotNull(person.id);

        Assertions.assertEquals(1, personDao.count());
        Assertions.assertEquals(1, personDao.count("name = ?1", "stef"));
        Assertions.assertEquals(1, personDao.count("name", "stef"));

        Assertions.assertEquals(1, dogDao.count());
        Assertions.assertEquals(1, person.dogs.size());

        persons = personDao.findAll();
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));
        
        persons = personDao.find("name = ?1", "stef");
        persons = personDao.find("name", "stef");
        Assertions.assertEquals(1, persons.size());
        Assertions.assertEquals(person, persons.get(0));
        
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

        return "OK";
    }
 
    private Person makeSavedPersonDao() {
        Person person = new Person();
        person.name = "stef";
        person.status = Status.LIVING;
        person.address = new Address("stef street");
        addressDao.save(person.address);

        personDao.save(person);

        Dog dog = new Dog("octave", "dalmatian");
        dog.owner = person;
        dogDao.save(dog);
        person.dogs.add(dog);
        
        return person;
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
