package io.quarkus.hibernate.orm.validation;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

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
}
