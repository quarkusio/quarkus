package io.quarkus.it.jpa;

import java.math.BigInteger;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
