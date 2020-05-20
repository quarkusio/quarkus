package io.quarkus.it.hibernate.orm.rest.data.panache.entity;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true)
public interface BookEntityResource extends PanacheEntityResource<BookEntity, Long> {

}
