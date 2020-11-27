package io.quarkus.it.hibernate.orm.rest.data.panache;

import java.util.List;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(exposed = false)
public interface AuthorsResource extends PanacheEntityResource<Author, Long> {

    @MethodProperties
    List<Author> list(Page page, Sort sort);

    @MethodProperties
    Author get(Long id);
}
