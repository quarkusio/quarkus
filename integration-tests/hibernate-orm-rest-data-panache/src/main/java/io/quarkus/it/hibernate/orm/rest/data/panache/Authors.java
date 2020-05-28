package io.quarkus.it.hibernate.orm.rest.data.panache;

import javax.ws.rs.core.Response;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.MethodProperties;

public interface Authors extends PanacheEntityResource<Author, Long> {

    @MethodProperties(exposed = false)
    Response add(Author entity);

    @MethodProperties(exposed = false)
    Response update(Long id, Author entity);

    @MethodProperties(exposed = false)
    void delete(Long id);
}
