package io.quarkus.it.hibernate.orm.rest.data.panache.repository;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true)
public interface BookPojoResource extends PanacheRepositoryResource<BookPojoRepository, BookPojo, Long> {

}
