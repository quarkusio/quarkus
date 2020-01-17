package io.quarkus.it.panache.rest.repository;

import io.quarkus.panache.rest.common.PanacheRestResource;
import io.quarkus.panache.rest.hibernate.orm.PanacheRepositoryCrudResource;

@PanacheRestResource(hal = true)
public interface BookPojoRepositoryCrudResource extends PanacheRepositoryCrudResource<BookPojoRepository, BookPojo, Long> {

}
