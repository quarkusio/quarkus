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

import org.junit.Assert;

import io.reactivex.Single;

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
        Assert.assertEquals(0, persons.size());
        
        Person person = makeSavedPerson();
        Assert.assertNotNull(person.id);

        Assert.assertEquals(1, Person.count());
        Assert.assertEquals(1, Person.count("name = ?1", "stef"));
        
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
        
        makeSavedPerson();
        Assert.assertEquals(1, Person.count());
        Assert.assertEquals(0, Person.delete("name = ?1", "emmanuel"));
        Assert.assertEquals(1, Person.delete("name = ?1", "stef"));

        Assert.assertEquals(0, Person.deleteAll());

        makeSavedPerson();
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
        return person;
    }

    @GET
    @Path("rxmodel")
    public Single<String> testRxModel() {
        return RxPerson.<RxPerson>findAll().toList()
            .flatMap(persons -> {
                Assert.assertEquals(0, persons.size());

                RxPerson person = new RxPerson();
                person.name = "stef";
                person.status = Status.LIVING;
//                person.address = new SequencedAddress("stef street");
//                person.address.save();
                return person.save().doOnError(t -> {
                    System.err.println("STEF");
                    t.printStackTrace();
                });
            }).flatMap(person -> {
                Assert.assertNotNull(person.id);
                
                return RxPerson.<RxPerson>findAll().toList()
                    .flatMapMaybe(persons -> {
                        Assert.assertEquals(1, persons.size());
                        Assert.assertEquals(person, persons.get(0));

                        return RxPerson.<RxPerson>findById(person.id);
                    }).map(byId -> {
                        Assert.assertEquals(person, byId);
                        return "OK";
                    }).toSingle();
            });
    }
}
