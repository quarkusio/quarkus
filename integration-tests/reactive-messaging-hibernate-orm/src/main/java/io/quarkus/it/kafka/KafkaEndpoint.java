package io.quarkus.it.kafka;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.kafka.common.TopicPartition;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.it.kafka.fruit.Fruit;
import io.quarkus.it.kafka.people.PeopleState;
import io.quarkus.it.kafka.people.Person;
import io.quarkus.smallrye.reactivemessaging.kafka.CheckpointEntityId;

@Path("/kafka")
public class KafkaEndpoint {
    @Inject
    KafkaReceivers receivers;

    @Inject
    @PersistenceUnit("people")
    EntityManager entityManager;

    @GET
    @Path("/fruits")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Fruit> getFruits() {
        return receivers.getFruits();
    }

    @GET
    @Path("/people")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Person> getPeople() {
        return receivers.getPeople();
    }

    @GET
    @Path("/people-state")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public PeopleState getPeopleState() {
        return entityManager.find(PeopleState.class,
                new CheckpointEntityId("people-checkpoint", new TopicPartition("people", 0)));
    }

}
