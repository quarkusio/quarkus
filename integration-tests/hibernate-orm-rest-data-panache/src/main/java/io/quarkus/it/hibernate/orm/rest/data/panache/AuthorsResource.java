package io.quarkus.it.hibernate.orm.rest.data.panache;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.MethodProperties;

public interface AuthorsResource extends PanacheEntityResource<Author, Long> {

    @MethodProperties(exposed = false)
    Author add(Author entity);

    @MethodProperties(exposed = false)
    Author update(Long id, Author entity);

    @MethodProperties(exposed = false)
    boolean delete(Long id);
}
