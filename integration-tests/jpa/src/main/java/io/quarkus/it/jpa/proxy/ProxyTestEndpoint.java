package io.quarkus.it.jpa.proxy;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.Hibernate;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
@Path("/proxy/")
@Produces(MediaType.TEXT_PLAIN)
public class ProxyTestEndpoint {

    @Inject
    EntityManager entityManager;

    @Transactional
    @TransactionConfiguration(timeoutFromConfigProperty = "dummy.transaction.timeout")
    public void setup(@Observes StartupEvent startupEvent) {
        ConcreteEntity entity = new ConcreteEntity();
        entity.id = "1";
        entity.type = "Concrete";
        entityManager.persist(entity);

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

    @GET
    @Path("basic")
    @Transactional
    public String testBasic() {
        final List list = entityManager.createQuery("from ConcreteEntity").getResultList();
        if (list.size() != 1) {
            throw new RuntimeException("Expected 1 result, got " + list.size());
        }
        return "OK";
    }

    /**
     * tests for the proxies in an inheritance hierarchy
     *
     * We need to do our own proxy generation at build time, so this tests that the logic matches what hibernate expects
     */
    // TODO: this previously was about the @Proxy annotation, removed in Hibernate ORM 7.0.
    //   We should check whether the test still makes sense...
    //   See also https://hibernate.zulipchat.com/#narrow/stream/132094-hibernate-orm-dev/topic/HHH-18194/near/467597335
    @GET
    @Path("inheritance")
    @Transactional
    public String inheritance() {
        PetOwner owner = entityManager.find(PetOwner.class, 1);
        expectEquals("Stuart", owner.getName());
        expectEquals("Generic pet noises", owner.getPet().makeNoise());

        owner = entityManager.find(PetOwner.class, 2);
        expectEquals("Sanne", owner.getName());
        expectEquals("Meow", owner.getPet().makeNoise());

        owner = entityManager.find(PetOwner.class, 3);
        expectEquals("Emmanuel", owner.getName());
        expectEquals("Woof", owner.getPet().makeNoise());
        var unproxied = Hibernate.unproxy(owner.getPet());
        expectTrue(unproxied instanceof Dog);
        expectEquals("Woof", ((Dog) unproxied).bark());
        expectEquals("Rubber Bone", ((Dog) unproxied).getFavoriteToy());

        return "OK";
    }

    @GET
    @Path("enhanced")
    public String testEnhanced() {
        //Define the test data:
        CompanyCustomer company = new CompanyCustomer();
        company.companyname = "Quarked consulting, inc.";
        Project project = new Project();
        project.name = "Hibernate RX";
        project.customer = company;

        //Store the test model:
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager.persist(project));
        final Integer testId = project.id;
        expectTrue(testId != null);

        //Now try to load it, should trigger the use of enhanced proxies:
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager.find(Project.class, testId));

        return "OK";
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
