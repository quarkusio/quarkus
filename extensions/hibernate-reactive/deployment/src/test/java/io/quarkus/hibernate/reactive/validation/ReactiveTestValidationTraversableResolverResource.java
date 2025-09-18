package io.quarkus.hibernate.reactive.validation;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

@Path("/validation")
@ApplicationScoped
public class ReactiveTestValidationTraversableResolverResource {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Inject
    Validator validator;

    @POST
    public Uni<Long> save() {
        return sessionFactory.withTransaction(s -> {
            MyLazyEntity entity = new MyLazyEntity();
            entity.setName("name");
            MyLazyChildEntity childEntity = new MyLazyChildEntity();
            childEntity.setParent(entity);
            childEntity.setName("123456789012345"); // should fail validation for the `SomeGroup` validation group.
            entity.setChildren(Set.of(childEntity));
            return s.persist(entity).map(v -> entity.getId());
        });
    }

    @GET
    @Path("/{id}")
    public Uni<String> testPass(@PathParam("id") Long id) {
        return sessionFactory.withTransaction(s -> s.find(MyLazyEntity.class, id)
                // since the Validator shouldn't fetch a lazy association, there should be no failures here:
                .map(e -> validator.validate(e, MyLazyChildEntity.SomeGroup.class).isEmpty() ? "OK" : "KO"));
    }

    @GET
    @Path("/fail/{id}")
    public Uni<String> testFail(@PathParam("id") Long id) {
        return sessionFactory.withTransaction(s -> s.find(MyLazyEntity.class, id)
                .chain(e -> Mutiny.fetch(e.getChildren()).map(c -> e))
                // Validating for a SomeGroup validation group should trigger a single constraint failure on a child object
                .map(e -> validator.validate(e, MyLazyChildEntity.SomeGroup.class).size() == 1 ? "OK" : "KO"));
    }

}
