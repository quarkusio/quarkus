package io.quarkus.it.jpa.h2.proxy;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import io.quarkus.runtime.StartupEvent;

@WebServlet(urlPatterns = "/jpa-h2/testproxy")
@ApplicationScoped
public class ProxyTestEndpoint extends HttpServlet {

    @Inject
    EntityManager entityManager;

    @Transactional
    @TransactionConfiguration(timeoutFromConfigProperty = "dummy.transaction.timeout")
    public void setup(@Observes StartupEvent startupEvent) {
        Pet pet = new Pet();
        pet.setId(1);
        pet.setName("Goose");

        PetOwner petOwner = new PetOwner();
        petOwner.setId(1);
        petOwner.setName("Stuart");
        petOwner.setPet(pet);

        entityManager.persist(petOwner);

        pet = new Cat();
        pet.setId(2);
        pet.setName("Tiddles");

        petOwner = new PetOwner();
        petOwner.setId(2);
        petOwner.setName("Sanne");
        petOwner.setPet(pet);

        entityManager.persist(petOwner);

        pet = new Dog();
        pet.setId(3);
        pet.setName("Spot");
        ((Dog) pet).setFavoriteToy("Rubber Bone");

        petOwner = new PetOwner();
        petOwner.setId(3);
        petOwner.setName("Emmanuel");
        petOwner.setPet(pet);

        entityManager.persist(petOwner);

    }

    /**
     * tests for the @Proxy annotation in an inheritance hierarchy
     *
     * We need to do our own proxy generation at build time, so this tests that the logic matches what hibernate expects
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PetOwner owner = entityManager.find(PetOwner.class, 1);
        expectEquals("Stuart", owner.getName());
        expectEquals("Generic pet noises", owner.getPet().makeNoise());
        expectFalse(owner.getPet() instanceof Pet);
        expectTrue(owner.getPet() instanceof DogProxy); //even though it is not a dog it still should implement the interface

        owner = entityManager.find(PetOwner.class, 2);
        expectEquals("Sanne", owner.getName());
        expectEquals("Meow", owner.getPet().makeNoise());

        DogProxy dogProxy = (DogProxy) owner.getPet();
        try {
            dogProxy.bark();
            throw new RuntimeException("Should have failed as not a dog");
        } catch (ClassCastException e) {

        }
        expectFalse(owner.getPet() instanceof Pet);

        owner = entityManager.find(PetOwner.class, 3);
        expectEquals("Emmanuel", owner.getName());
        expectEquals("Woof", owner.getPet().makeNoise());
        expectEquals("Woof", ((DogProxy) owner.getPet()).bark());
        expectEquals("Rubber Bone", ((DogProxy) owner.getPet()).getFavoriteToy());
        expectFalse(owner.getPet() instanceof Pet);
        expectTrue(owner.getPet() instanceof DogProxy);

        resp.getWriter().write("OK");

    }

    void expectEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new RuntimeException("Expected " + expected + " but was " + actual);
        }
    }

    void expectTrue(boolean val) {
        if (!val) {
            throw new RuntimeException("Assertion failed");
        }
    }

    void expectFalse(boolean val) {
        if (val) {
            throw new RuntimeException("Assertion failed");
        }
    }
}
