package io.quarkus.it.panache.rest.repository;

import io.quarkus.panache.rest.common.ResourceProperties;
import io.quarkus.panache.rest.hibernate.orm.PanacheRepositoryCrudResource;

@ResourceProperties(hal = true)
public interface BookPojoResource extends PanacheRepositoryCrudResource<BookPojoRepository, BookPojo, Long> {

}
