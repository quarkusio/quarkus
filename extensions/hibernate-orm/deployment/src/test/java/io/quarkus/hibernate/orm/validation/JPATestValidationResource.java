package io.quarkus.hibernate.orm.validation;

import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import io.quarkus.hibernate.orm.MyEntity;

@Path("/validation")
@ApplicationScoped
public class JPATestValidationResource {

    @Inject
    EntityManager em;

    @POST
    @Transactional
    public String save(String name) {
        MyEntity entity = new MyEntity();
        entity.setName(name);
        try {
            em.persist(entity);
            em.flush();
            return "OK";
        } catch (ConstraintViolationException exception) {
            return exception.getConstraintViolations()
                    .stream()
                    .map(s -> s.getMessage())
                    .collect(Collectors.joining());
        }
    }

    @GET
    public String ddl() {
        return "nullable: " + em.getEntityManagerFactory()
                .unwrap(SessionFactoryImplementor.class)
                .getMappingMetamodel()
                .getEntityDescriptor(MyEntity.class)
                .getEntityMappingType()
                .getAttributeMapping(0)
                .getAttributeMetadata()
                .isNullable();
    }
}
