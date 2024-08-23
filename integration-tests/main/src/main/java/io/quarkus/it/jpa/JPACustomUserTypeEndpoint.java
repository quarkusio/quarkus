package io.quarkus.it.jpa;

import java.math.BigInteger;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/jpa/custom-user-types")
public class JPACustomUserTypeEndpoint {

    @Inject
    EntityManager em;

    @GET
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Long invokeCreation() {
        CustomTypeEntity customTypeEntity = new CustomTypeEntity();
        customTypeEntity.setBigInteger(BigInteger.ONE);
        customTypeEntity.setCustomEnum(CustomEnum.ONE);
        Animal animal = new Animal();
        animal.setWeight(29.12);
        customTypeEntity.setAnimal(animal);
        em.persist(customTypeEntity);
        em.flush();
        return customTypeEntity.getId();
    }
}
