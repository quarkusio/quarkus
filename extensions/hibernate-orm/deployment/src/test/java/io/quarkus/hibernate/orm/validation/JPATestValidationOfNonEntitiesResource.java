package io.quarkus.hibernate.orm.validation;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/validation/nonentity")
@ApplicationScoped
public class JPATestValidationOfNonEntitiesResource {

    @Inject
    Validator validator;

    @Path("/bean")
    @GET
    public String bean() {
        Set<ConstraintViolation<MyNonEntity>> constraintViolations = validator.validate(new MyNonEntity());
        if (constraintViolations.size() != 1) {
            return "ko";
        }
        if (!constraintViolations.iterator().next().getPropertyPath().toString().equals("name")) {
            return "ko";
        }
        return "ok";
    }

    @Path("/value")
    @GET
    public String value() {
        Set<ConstraintViolation<MyNonEntity>> constraintViolations = validator.validateValue(MyNonEntity.class, "name", null);
        if (constraintViolations.size() != 1) {
            return "ko";
        }
        if (!constraintViolations.iterator().next().getPropertyPath().toString().equals("name")) {
            return "ko";
        }
        return "ok";
    }

    public static class MyNonEntity {
        @NotNull
        public String name;
    }
}
