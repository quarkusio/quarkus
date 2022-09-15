package io.quarkus.smallrye.openapi.test.jaxrs;

import java.util.List;

import jakarta.ws.rs.Path;

@Path("/tagged")
public class AutoTagResource extends AutoTagFetchableResource<String> {

    @Override
    public String getById(long id) {
        return "Disney Land, Gate " + id;
    }

    @Override
    public List<String> getAll() {
        return null;
    }

}
