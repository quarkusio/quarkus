package io.quarkus.it.panache.resources;

import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.it.panache.custompu.CustomPuEntity;

@Path("/custom-pu")
public class CustomPuResource {

    @Transactional
    @POST
    @Path("/{string}")
    public CustomPuEntity create(@PathParam("string") String string) {
        CustomPuEntity entity = new CustomPuEntity();
        entity.string = string;
        entity.persist();
        return entity;
    }

    @Transactional
    @PATCH
    @Path("/{string}")
    public List<CustomPuEntity> updateAll(@PathParam("string") String string) {
        CustomPuEntity.update("set string = ?1 where 1 = 1", string);
        return CustomPuEntity.findAll().list();
    }
}
