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

import java.util.List;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.panache.Controller;
import org.jboss.panache.router.Router;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.junit.Assert;

import io.reactivex.Single;
import io.reactivex.SingleSource;

/**
 * Various tests covering Panache functionality. All tests should work in both standard JVM and SubstrateVM.
 */
@Path("test")
public class TestEndpoint extends Controller {

    @GET
    @Path("model")
    @Transactional
    public String testModel() {
        List<Person> persons = Person.findAll();
        Assert.assertEquals(0, persons.size());
        
        Person person = makeSavedPerson();
        Assert.assertNotNull(person.id);

        Assert.assertEquals(1, Person.count());
        Assert.assertEquals(1, Person.count("name = ?1", "stef"));

        Assert.assertEquals(1, Dog.count());
        Assert.assertEquals(1, person.dogs.size());

        persons = Person.findAll();
        Assert.assertEquals(1, persons.size());
        Assert.assertEquals(person, persons.get(0));
        
        persons = Person.find("name = ?1", "stef");
        Assert.assertEquals(1, persons.size());
        Assert.assertEquals(person, persons.get(0));
        
        Person byId = Person.findById(person.id);
        Assert.assertEquals(person, byId);

        person.delete();
        Assert.assertEquals(0, Person.count());
        
        person = makeSavedPerson();
        Assert.assertEquals(1, Person.count());
        Assert.assertEquals(0, Person.delete("name = ?1", "emmanuel"));
        Assert.assertEquals(1, Dog.delete("owner = ?1", person));
        Assert.assertEquals(1, Person.delete("name = ?1", "stef"));

        Assert.assertEquals(0, Person.deleteAll());

        makeSavedPerson();
        Assert.assertEquals(1, Dog.deleteAll());
        Assert.assertEquals(1, Person.deleteAll());

        return "OK";
    }

    private Person makeSavedPerson() {
        Person person = new Person();
        person.name = "stef";
        person.status = Status.LIVING;
        person.address = new SequencedAddress("stef street");
        person.address.save();
        
        person.save();

        Dog dog = new Dog("octave", "dalmatian");
        dog.owner = person;
        dog.save();
        person.dogs.add(dog);
        
        return person;
    }

    @GET
    @Path("rxmodel")
    public Single<String> testRxModel() {
        return RxPerson.findAll().toList()
            .flatMap(persons -> {
                Assert.assertEquals(0, persons.size());

                return makeSavedRxPerson();
            }).flatMap(person -> {
                Assert.assertNotNull(person.id);

                return RxPerson.findAll().toList()
                    .flatMapMaybe(persons -> {
                        Assert.assertEquals(1, persons.size());
                        Assert.assertEquals(person, persons.get(0));

                        return RxPerson.findById(person.id);
                    }).flatMapSingle(byId -> {
                        Assert.assertEquals(person, byId);

                        return RxPerson.find("name = ?1", "stef").toList();
                    }).flatMap(persons -> {
                        Assert.assertEquals(1, persons.size());
                        Assert.assertEquals(person, persons.get(0));

                        return RxPerson.find("name = ?1", "emmanuel").toList();
                    }).flatMap(persons -> {
                        Assert.assertEquals(0, persons.size());

                        return RxPerson.count();
                    }).flatMap(count -> {
                        Assert.assertEquals(1, (long)count);
                        
                        return RxPerson.count("name = ?1", "stef");
                    }).flatMapCompletable(count -> {
                        Assert.assertEquals(1, (long)count);

                        return person.delete();
                    })
                    .andThen(Single.defer(() -> RxPerson.count()))
                    .flatMap(count -> {
                        Assert.assertEquals(0, (long)count);
                        
                        return makeSavedRxPerson();
                    })
                    .flatMap(p -> RxPerson.count())
                    .flatMap(count -> {
                        Assert.assertEquals(1, (long)count);
                        
                        return RxPerson.deleteAll();
                    }).flatMap(count -> {
                        Assert.assertEquals(1, (long)count);

                        return RxPerson.count();
                    }).flatMap(count -> {
                        Assert.assertEquals(0, (long)count);
                        
                        return makeSavedRxPerson();
                    }).flatMap(p ->  RxPerson.delete("name = ?1", "emmanuel"))
                    .flatMap(count -> {
                        Assert.assertEquals(0, (long)count);
                        
                        return RxPerson.delete("name = ?1", "stef");
                    }).flatMap(count -> {
                        Assert.assertEquals(1, (long)count);
                        
                        return makeSavedRxPerson();
                    }).flatMap(p -> {
                        
                        RxDog dog = new RxDog("octave", "dalmatian");
                        dog.owner = Single.just(p);
                        return dog.save();
                    }).flatMapMaybe(d -> {
                        // get it from the DB
                        return RxDog.<RxDog>findById(d.id);
                        // check the lazy single
                    }).flatMapSingle(d -> d.owner)
                    // check the lazy list
                    .flatMap(p -> p.dogs.toList())
                    .map(dogs -> {
                        Assert.assertEquals(1, dogs.size());
                        
                        return "OK";
                    });
            });
    }

    private Single<? extends RxPerson> makeSavedRxPerson() {
        RxPerson person = new RxPerson();
        person.name = "stef";
        person.status = Status.LIVING;
//        person.address = new SequencedAddress("stef street");
//        person.address.save();
        try {
            return person.save();
        }catch(Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @GET
    @Path("router-test/{pathParam}")
    public String testMethod1(@PathParam String pathParam) {
        return "Hello";
    }

    @GET
    @Path("router-test/{p1}-{p2}")
    public String testMethod2(@PathParam int p1,
            @PathParam byte[] p2) {
        return "Hello";
    }

    @GET
    @Path("router")
    public String testRouter() {
        
        Assert.assertEquals("http://localhost:8080/api/test/router", Router.getURI(TestEndpoint::testRouter).toString());
        Assert.assertEquals("http://localhost:8080/api/test/router-test/stef", Router.getURI(TestEndpoint::testMethod1, "stef").toString());
        Assert.assertTrue(Router.getURI(TestEndpoint::testMethod2, 2, new byte[] {20}).toString()
                .startsWith("http://localhost:8080/api/test/router-test/2-"));
        
        return "OK";
    }
}
