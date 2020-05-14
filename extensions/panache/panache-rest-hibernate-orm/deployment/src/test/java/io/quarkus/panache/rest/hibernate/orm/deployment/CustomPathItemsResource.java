package io.quarkus.panache.rest.hibernate.orm.deployment;

import java.util.List;

import javax.ws.rs.core.Response;

import io.quarkus.panache.rest.common.OperationProperties;
import io.quarkus.panache.rest.common.ResourceProperties;
import io.quarkus.panache.rest.hibernate.orm.PanacheEntityCrudResource;

@ResourceProperties(path = "custom-items", hal = true)
public interface CustomPathItemsResource extends PanacheEntityCrudResource<Item, Long> {

    @OperationProperties(path = "api")
    List<Item> list();

    @OperationProperties(path = "api")
    Item get(Long id);

    @OperationProperties(path = "api")
    Response add(Item entity);

    @OperationProperties(path = "api")
    Response update(Long id, Item entity);

    @OperationProperties(path = "api")
    void delete(Long id);
}
