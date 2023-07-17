package io.quarkus.it.validator;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Verifies that Hibernate Validator can use Hibernate ORM's PersistenceUtilHelper
 * to traverse managed object graphs which contain enhanced proxies.
 *
 * Test for issue https://github.com/quarkusio/quarkus/issues/8323
 */
@Path("/validator-and-orm")
public class TestValidatorAndHibernateORMEndpoint {

    @Inject
    EntityManager em;

    @Inject
    Validator validator;

    @GET
    @Path("/store")
    @Transactional
    public String storeData() {
        //Create hello#1
        Hello hello = new Hello();
        hello.setId(1);
        hello.setGreetingText("hello");
        //Create human#3
        Human human = new Human();
        human.setId(3);
        human.setName("Batman");
        //create the relation
        human.getGreetings().add(hello);
        hello.setGreetedHuman(human);
        //Store it all
        em.persist(human);
        return "passed";
    }

    @GET
    @Path("/load")
    @Transactional
    public String loadData() {
        //Load hello#1 - this will lead to have an uninitialized enhanced proxy associated to it
        Hello hello = em.find(Hello.class, 1);
        //Check that we can still validate the managed object graph, even when we don't know the name of the guy in the mask who we just greeted!
        validator.validate(hello);
        return "passed";
    }

}
