package io.quarkus.smallrye.openapi.test.jaxrs;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Path;

@Path("/child")
public class ChildResource extends BaseResource<String> {

    @RolesAllowed("admin")
    @Override
    public List<String> list() {
        return super.list();
    }

    @RolesAllowed("user")
    @Override
    public String create(String entity) {
        return super.create(entity);
    }

    @Override
    public long count() {
        return super.count();
    }
}
