package io.quarkus.hibernate.reactive.validation;

import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

@Path("/validation")
@ApplicationScoped
public class ReactiveTestValidationResource {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @POST
    public Uni<String> save(String name) {
        return sessionFactory.withTransaction(s -> {
            MyEntity entity = new MyEntity();
            entity.setName(name);
            return s.persist(entity);
        }).onItemOrFailure()
                .transform((v, ex) -> {
                    if (ex != null) {
                        Throwable e = ex;
                        while (e != null && !(e instanceof ConstraintViolationException)) {
                            e = e.getCause();
                        }
                        if (e instanceof ConstraintViolationException ce) {
                            return ce.getConstraintViolations()
                                    .stream()
                                    .map(ConstraintViolation::getMessage)
                                    .collect(Collectors.joining());
                        } else {
                            return ex.getMessage();
                        }
                    }
                    return "OK";
                });
    }

    @GET
    public Uni<String> ddl() {
        // there's no access to mapping metamodel from the reactive session, so let's try an insert
        // with a query and see how it either passes or fails, to test the validation-to-ddl part:
        return sessionFactory
                .withTransaction(s -> s.createNativeQuery("insert into my_entity_table(name) values (null)").executeUpdate())
                .onItemOrFailure()
                .transform((v, ex) -> {
                    if (ex != null) {
                        return "nullable: false";
                    }
                    return "nullable: true";
                });
    }
}
