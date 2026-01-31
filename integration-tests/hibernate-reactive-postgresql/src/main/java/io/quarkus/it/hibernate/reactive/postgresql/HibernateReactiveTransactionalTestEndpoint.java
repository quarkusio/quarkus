package io.quarkus.it.hibernate.reactive.postgresql;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;

@Path("/transactional-tests")
@Authenticated
public class HibernateReactiveTransactionalTestEndpoint {

    @Inject
    Mutiny.Session session;

    @POST
    @Path("/createPig/{id}")
    public Uni<GuineaPig> reactiveTransactionalCreatePig(int id) {
        final GuineaPig expectedPig = new GuineaPig(id, "initialName");
        return createFindPig(expectedPig);
    }

    @POST
    @Path("/updatePig/{pigId}")
    public Uni<Void> reactiveTransactionalUpdatePig(int pigId, @QueryParam("name") String name) {
        return updatePig(pigId, name);
    }

    @GET
    @Path("/findPig/{pigId}")
    public Uni<GuineaPig> reactiveTransactionalFindPig(int pigId) {
        return transactionalFind(pigId);
    }

    @POST
    @Path("/updatePigHanging/{pigId}")
    public Uni<Void> reactiveTransactionalUpdatePigHanging(int pigId, @QueryParam("name") String name) {
        return updatePigHanging(pigId, name);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public Uni<GuineaPig> transactionalFind(int pigId) {
        return session.find(GuineaPig.class, pigId);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public Uni<GuineaPig> createFindPig(GuineaPig pig) {
        return session.persist(pig)
                .chain(p -> session.find(GuineaPig.class, pig.getId()));
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public Uni<Void> updatePig(int pigId, String newName) {
        return session.find(GuineaPig.class, pigId)
                .map(p -> {
                    p.setName(newName);
                    return p;
                })
                .replaceWithVoid();
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public Uni<Void> updatePigHanging(int pigId, String newName) {
        return session.find(GuineaPig.class, pigId)
                .map(p -> {
                    p.setName(newName);
                    return p;
                }).chain(() -> session.flush())
                .chain(() -> {
                    // This will make the endpoint never complete
                    return Uni.createFrom().nothing();
                })
                .replaceWithVoid();
    }
}